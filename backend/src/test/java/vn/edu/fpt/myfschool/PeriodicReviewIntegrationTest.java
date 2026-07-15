package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.enums.*;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.SemesterResultCalculationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PeriodicReviewIntegrationTest extends BaseIntegrationTest {
    @Autowired private SubjectStudentReviewRepository subjectReviewRepository;
    @Autowired private StudentPeriodicReportRepository periodicReportRepository;
    @Autowired private StudentReviewAuditRepository reviewAuditRepository;
    @Autowired private SemesterResultRepository semesterResultRepository;
    @Autowired private GradeBookRepository gradeBookRepository;
    @Autowired private GradeItemRepository gradeItemRepository;
    @Autowired private StudentScoreRepository studentScoreRepository;
    @Autowired private SemesterResultCalculationService semesterResultCalculationService;

    private String teacherToken;
    private HomeroomAssignment homeroom;

    @BeforeEach
    void setupReviewScope() throws Exception {
        teacherToken = loginAsTeacher();
        homeroom = new HomeroomAssignment();
        homeroom.setAcademicYear(testAcademicYear);
        homeroom.setCls(testClass);
        homeroom.setTeacher(testTeacher);
        homeroom.setEffectiveFrom(LocalDate.now().minusDays(1));
        homeroom = homeroomAssignmentRepository.save(homeroom);
    }

    @Test
    void subjectTeacherCannotUseAnotherAssignmentOrEditAfterSubmit() throws Exception {
        Subject outside = createSubject("LY12", "Vật lý");
        mockMvc.perform(put("/api/subject-reviews/{studentId}", testStudent1.getId())
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjectReviewBody(testAcademicYear.getId(), testSemester.getId(), testClass.getId(),
                                outside.getId(), "Không thuộc phân công")))
                .andExpect(status().isForbidden());

        saveReview(testStudent1.getId(), "Có tiến bộ rõ rệt");
        submitReviews(List.of(testStudent1.getId()));

        mockMvc.perform(put("/api/subject-reviews/{studentId}", testStudent1.getId())
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjectReviewBody(testAcademicYear.getId(), testSemester.getId(), testClass.getId(),
                                testSubject.getId(), "Sửa sau submit")))
                .andExpect(status().isConflict());
    }

    @Test
    void homeroomTeacherOnlySeesOwnClassAndReturnRequiresReason() throws Exception {
        saveReview(testStudent1.getId(), "Hoàn thành yêu cầu");
        submitReviews(List.of(testStudent1.getId()));
        Long reviewId = subjectReviewRepository
                .findByStudentIdAndSubjectIdAndSemesterId(testStudent1.getId(), testSubject.getId(), testSemester.getId())
                .orElseThrow().getId();

        mockMvc.perform(put("/api/subject-reviews/{id}/return", reviewId)
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"academicYearId\":" + testAcademicYear.getId() + ",\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/subject-reviews/{id}/return", reviewId)
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"academicYearId\":" + testAcademicYear.getId()
                                + ",\"reason\":\"Cần nhận xét cụ thể hơn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETURNED"))
                .andExpect(jsonPath("$.data.returnReason").value("Cần nhận xét cụ thể hơn"));

        Teacher otherTeacher = createTeacher("0909000088", "GV Khác", "GV200");
        String otherToken = login("0909000088", "test1234");
        mockMvc.perform(get("/api/homeroom-reports")
                        .header("Authorization", authHeader(otherToken))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString())
                        .param("classId", testClass.getId().toString()))
                .andExpect(status().isForbidden());
        assertThat(otherTeacher.getId()).isNotNull();
    }

    @Test
    void draftIsPrivateAndPublishUpdatesOfficialConductWithCompleteAudit() throws Exception {
        saveReview(testStudent1.getId(), "Chủ động trong học tập");
        submitReviews(List.of(testStudent1.getId()));
        saveHomeroomDraft(testStudent1.getId(), "Em có ý thức học tập tốt", "Tốt");

        mockMvc.perform(get("/api/periodic-reports/students/{studentId}", testStudent1.getId())
                        .header("Authorization", authHeader(loginAsParent()))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString()))
                .andExpect(status().isNotFound());

        publishStudent(testStudent1.getId());

        mockMvc.perform(get("/api/periodic-reports/students/{studentId}", testStudent1.getId())
                        .header("Authorization", authHeader(loginAsParent()))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.conduct").value("Tốt"))
                .andExpect(jsonPath("$.data.subjectReviews[0].comment").value("Chủ động trong học tập"));

        mockMvc.perform(get("/api/periodic-reports/students/{studentId}", testStudent1.getId())
                        .header("Authorization", authHeader(loginAsStudent2()))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString()))
                .andExpect(status().isForbidden());

        Long reviewId = subjectReviewRepository
                .findByStudentIdAndSubjectIdAndSemesterId(testStudent1.getId(), testSubject.getId(), testSemester.getId())
                .orElseThrow().getId();
        mockMvc.perform(put("/api/subject-reviews/{id}/return", reviewId)
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"academicYearId\":" + testAcademicYear.getId()
                                + ",\"reason\":\"Không được lộ nhận xét nội bộ\"}"))
                .andExpect(status().isConflict());

        SemesterResult result = semesterResultRepository
                .findByStudentIdAndSemesterId(testStudent1.getId(), testSemester.getId()).orElseThrow();
        assertThat(result.getConduct()).isEqualTo("Tốt");
        assertThat(result.getConductSource()).isEqualTo(ConductSource.HOMEROOM);

        StudentPeriodicReport report = periodicReportRepository
                .findByStudentIdAndSemesterId(testStudent1.getId(), testSemester.getId()).orElseThrow();
        List<StudentReviewAudit> auditRows = reviewAuditRepository
                .findByEntityTypeAndEntityIdOrderByChangedAtDesc("PERIODIC_REPORT", report.getId());
        assertThat(auditRows).isNotEmpty();
        assertThat(auditRows.getFirst().getOldValueJson()).isNotBlank();
        assertThat(auditRows.getFirst().getNewValueJson()).contains("PUBLISHED");
        assertThat(auditRows.getFirst().getChangedBy().getId()).isEqualTo(testTeacher.getUser().getId());
        assertThat(auditRows.getFirst().getChangedAt()).isNotNull();
        assertThat(auditRows.getFirst().getReason()).isEqualTo("PUBLISH");
    }

    @Test
    void recalculatingSemesterResultsDoesNotOverwriteHomeroomConduct() throws Exception {
        saveReview(testStudent1.getId(), "Đạt yêu cầu");
        submitReviews(List.of(testStudent1.getId()));
        saveHomeroomDraft(testStudent1.getId(), "Cần duy trì", "Khá");
        publishStudent(testStudent1.getId());
        createCompleteGradeBook();

        semesterResultCalculationService.calculate(testClass.getId(), testSemester.getId());

        SemesterResult result = semesterResultRepository
                .findByStudentIdAndSemesterId(testStudent1.getId(), testSemester.getId()).orElseThrow();
        assertThat(result.getConduct()).isEqualTo("Khá");
        assertThat(result.getConductSource()).isEqualTo(ConductSource.HOMEROOM);
        assertThat(result.getSuggestedConduct()).isEqualTo("Tốt");
    }

    @Test
    void adminReopenRequiresReasonAndHidesReportAgain() throws Exception {
        saveReview(testStudent1.getId(), "Đạt yêu cầu");
        submitReviews(List.of(testStudent1.getId()));
        saveHomeroomDraft(testStudent1.getId(), "Hoàn thành học kỳ", "Tốt");
        publishStudent(testStudent1.getId());
        StudentPeriodicReport report = periodicReportRepository
                .findByStudentIdAndSemesterId(testStudent1.getId(), testSemester.getId()).orElseThrow();
        String adminToken = loginAsAdmin();

        mockMvc.perform(put("/api/periodic-reports/admin/{id}/reopen", report.getId())
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"academicYearId\":" + testAcademicYear.getId() + ",\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/periodic-reports/admin/{id}/reopen", report.getId())
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"academicYearId\":" + testAcademicYear.getId()
                                + ",\"reason\":\"Điều chỉnh theo biên bản họp\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(get("/api/periodic-reports/students/{studentId}", testStudent1.getId())
                        .header("Authorization", authHeader(loginAsParent()))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString()))
                .andExpect(status().isNotFound());

        SemesterResult result = semesterResultRepository
                .findByStudentIdAndSemesterId(testStudent1.getId(), testSemester.getId()).orElseThrow();
        assertThat(result.getConductSource()).isEqualTo(ConductSource.SUGGESTED);
        assertThat(reviewAuditRepository
                .findByEntityTypeAndEntityIdOrderByChangedAtDesc("PERIODIC_REPORT", report.getId()).getFirst()
                .getReason()).isEqualTo("Điều chỉnh theo biên bản họp");
    }

    @Test
    void academicYearsAreIsolatedAndTransferredStudentUsesNewClassOnly() throws Exception {
        AcademicYear otherYear = new AcademicYear();
        otherYear.setName("2027-2028");
        otherYear.setStartDate(LocalDate.of(2027, 8, 1));
        otherYear.setEndDate(LocalDate.of(2028, 5, 31));
        otherYear.setStatus(AcademicYearStatus.DRAFT);
        otherYear = academicYearRepository.save(otherYear);
        Semester otherSemester = new Semester();
        otherSemester.setName("HK I");
        otherSemester.setOrder(1);
        otherSemester.setStartDate(LocalDate.of(2027, 9, 1));
        otherSemester.setEndDate(LocalDate.of(2028, 1, 15));
        otherSemester.setAcademicYear(otherYear);
        otherSemester = semesterRepository.save(otherSemester);

        mockMvc.perform(get("/api/subject-reviews")
                        .header("Authorization", authHeader(teacherToken))
                        .param("academicYearId", otherYear.getId().toString())
                        .param("semesterId", otherSemester.getId().toString())
                        .param("classId", testClass.getId().toString())
                        .param("subjectId", testSubject.getId().toString()))
                .andExpect(status().isForbidden());

        SchoolClass newClass = classRepository.findByNameAndAcademicYearId("SE1913", testAcademicYear.getId())
                .orElseThrow();
        Enrollment oldEnrollment = enrollmentRepository
                .findByStudentIdAndAcademicYearIdAndStatus(testStudent3.getId(), testAcademicYear.getId(),
                        EnrollmentStatus.ACTIVE).orElseThrow();
        oldEnrollment.setStatus(EnrollmentStatus.TRANSFERRED);
        oldEnrollment.setLeaveDate(LocalDate.now());
        enrollmentRepository.save(oldEnrollment);
        Enrollment moved = new Enrollment();
        moved.setStudent(testStudent3);
        moved.setAcademicYear(testAcademicYear);
        moved.setCls(newClass);
        moved.setJoinDate(LocalDate.now());
        moved.setStatus(EnrollmentStatus.ACTIVE);
        enrollmentRepository.save(moved);
        testStudent3.setCurrentClass(newClass);
        studentRepository.save(testStudent3);
        TeachingAssignment movedAssignment = new TeachingAssignment();
        movedAssignment.setCls(newClass);
        movedAssignment.setSubject(testSubject);
        movedAssignment.setTeacher(testTeacher);
        movedAssignment.setEffectiveFrom(testSemester.getStartDate());
        movedAssignment.setStatus(AssignmentStatus.ACTIVE);
        teachingAssignmentRepository.save(movedAssignment);

        mockMvc.perform(put("/api/subject-reviews/{studentId}", testStudent3.getId())
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjectReviewBody(testAcademicYear.getId(), testSemester.getId(), testClass.getId(),
                                testSubject.getId(), "Sai lớp cũ")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/subject-reviews/{studentId}", testStudent3.getId())
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjectReviewBody(testAcademicYear.getId(), testSemester.getId(), newClass.getId(),
                                testSubject.getId(), "Đúng lớp mới")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classId").value(newClass.getId()));
    }

    private void saveReview(Long studentId, String comment) throws Exception {
        mockMvc.perform(put("/api/subject-reviews/{studentId}", studentId)
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjectReviewBody(testAcademicYear.getId(), testSemester.getId(), testClass.getId(),
                                testSubject.getId(), comment)))
                .andExpect(status().isOk());
    }

    private void submitReviews(List<Long> studentIds) throws Exception {
        mockMvc.perform(post("/api/subject-reviews/submit")
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "academicYearId", testAcademicYear.getId(), "semesterId", testSemester.getId(),
                                "classId", testClass.getId(), "subjectId", testSubject.getId(),
                                "studentIds", studentIds))))
                .andExpect(status().isOk());
    }

    private void saveHomeroomDraft(Long studentId, String comment, String conduct) throws Exception {
        mockMvc.perform(put("/api/homeroom-reports/students/{studentId}", studentId)
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "academicYearId", testAcademicYear.getId(), "semesterId", testSemester.getId(),
                                "classId", testClass.getId(), "generalComment", comment, "conduct", conduct))))
                .andExpect(status().isOk());
    }

    private void publishStudent(Long studentId) throws Exception {
        mockMvc.perform(post("/api/homeroom-reports/students/{studentId}/publish", studentId)
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "academicYearId", testAcademicYear.getId(), "semesterId", testSemester.getId(),
                                "classId", testClass.getId()))))
                .andExpect(status().isOk());
    }

    private String subjectReviewBody(Long yearId, Long semesterId, Long classId, Long subjectId, String comment)
            throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("academicYearId", yearId, "semesterId", semesterId,
                "classId", classId, "subjectId", subjectId, "comment", comment,
                "strengths", "Điểm mạnh", "improvements", "Điểm cần cải thiện"));
    }

    private Subject createSubject(String code, String name) {
        Subject subject = new Subject();
        subject.setCode(code);
        subject.setName(name);
        return subjectRepository.save(subject);
    }

    private Teacher createTeacher(String phone, String name, String code) {
        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("test1234"));
        user.setName(name);
        user.setRole(UserRole.TEACHER);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setEmployeeCode(code);
        return teacherRepository.save(teacher);
    }

    private void createCompleteGradeBook() {
        GradeBook book = new GradeBook();
        book.setCls(testClass);
        book.setSubject(testSubject);
        book.setSemester(testSemester);
        book = gradeBookRepository.save(book);
        GradeItem item = new GradeItem();
        item.setGradeBook(book);
        item.setName("Tổng kết");
        item.setCode("TK");
        item.setWeight(1);
        item.setOrder(0);
        item.setAssessmentType(AssessmentType.SCORE);
        item.setRequiredEntry(true);
        item = gradeItemRepository.save(item);
        for (Student student : List.of(testStudent1, testStudent2, testStudent3)) {
            StudentScore score = new StudentScore();
            score.setGradeItem(item);
            score.setStudent(student);
            score.setScore(BigDecimal.valueOf(8));
            score.setIsGraded(true);
            studentScoreRepository.save(score);
        }
    }
}
