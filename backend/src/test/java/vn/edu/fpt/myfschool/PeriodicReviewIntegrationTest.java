package vn.edu.fpt.myfschool;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.edu.fpt.myfschool.common.enums.ConductSource;
import vn.edu.fpt.myfschool.common.enums.PeriodicReportStatus;
import vn.edu.fpt.myfschool.common.enums.StudentEventStatus;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.entity.SemesterResult;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.StudentPeriodicReport;
import vn.edu.fpt.myfschool.repository.SemesterResultRepository;
import vn.edu.fpt.myfschool.repository.StudentEventRepository;
import vn.edu.fpt.myfschool.repository.StudentPeriodicReportRepository;
import vn.edu.fpt.myfschool.repository.StudentReviewAuditRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PeriodicReviewIntegrationTest extends BaseIntegrationTest {
    @Autowired private StudentPeriodicReportRepository periodicReports;
    @Autowired private StudentEventRepository studentEvents;
    @Autowired private SemesterResultRepository semesterResults;
    @Autowired private StudentReviewAuditRepository audits;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    private String teacherToken;

    @BeforeEach
    void setupHomeroom() throws Exception {
        teacherToken = loginAsTeacher();
        HomeroomAssignment homeroom = new HomeroomAssignment();
        homeroom.setAcademicYear(testAcademicYear);
        homeroom.setCls(testClass);
        homeroom.setTeacher(testTeacher);
        homeroom.setEffectiveFrom(LocalDate.now().minusDays(1));
        homeroomAssignmentRepository.save(homeroom);
    }

    @Test
    void subjectTeacherSubmitsDirectlyAndCannotBeReturnedOrEdited() throws Exception {
        saveSubjectReview(testStudent1, "Có tiến bộ rõ rệt");
        submitSubjectReviews(List.of(testStudent1.getId()));

        mockMvc.perform(put("/api/subject-reviews/{studentId}", testStudent1.getId())
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjectReviewBody(testStudent1, "Sửa sau Submit")))
                .andExpect(status().isConflict());

    }

    @Test
    void homeroomSubmitIncludesViolationsAndKeepsThemInternal() throws Exception {
        createViolation(testStudent1, "Đi học muộn");
        saveHomeroomDraft(testStudent1, "Cần cải thiện nề nếp");
        submitHomeroom(testStudent1);

        StudentPeriodicReport report = periodicReports
                .findByStudentIdAndSemesterId(testStudent1.getId(), testSemester.getId()).orElseThrow();
        assertThat(report.getStatus()).isEqualTo(PeriodicReportStatus.SUBMITTED);
        assertThat(studentEvents.findByStudentIdAndAcademicYearIdAndSemesterIdOrderByEventDateDesc(
                testStudent1.getId(), testAcademicYear.getId(), testSemester.getId()))
                .allMatch(event -> event.getStatus() == StudentEventStatus.SUBMITTED);

        mockMvc.perform(get("/api/students/{studentId}/events", testStudent1.getId())
                        .header("Authorization", authHeader(loginAsParent()))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/periodic-reports/students/{studentId}", testStudent1.getId())
                        .header("Authorization", authHeader(loginAsParent()))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString()))
                .andExpect(status().isNotFound());

        assertThat(audits.findByEntityTypeAndEntityIdOrderByChangedAtDesc("PERIODIC_REPORT", report.getId()))
                .anyMatch(row -> "SUBMIT_TO_ADMIN".equals(row.getReason()));
    }

    @Test
    void adminPublishesUnifiedResultsAfterAllIndependentSubmissions() throws Exception {
        List<Student> students = List.of(testStudent1, testStudent2, testStudent3);
        for (Student student : students) {
            saveSubjectReview(student, "Nhận xét môn của " + student.getStudentCode());
        }
        submitSubjectReviews(students.stream().map(Student::getId).toList());
        for (Student student : students) {
            saveHomeroomDraft(student, "Nhận xét chung của " + student.getStudentCode());
            submitHomeroom(student);
            createSemesterResult(student, students.indexOf(student) + 1);
        }
        createViolation(testStudent1, "Không làm bài tập");
        // Vi phạm thêm sau Submit vẫn ở nháp, nên không được tính cho đến lần Submit tiếp theo.
        StudentPeriodicReport firstReport = periodicReports
                .findByStudentIdAndSemesterId(testStudent1.getId(), testSemester.getId()).orElseThrow();
        firstReport.setStatus(PeriodicReportStatus.DRAFT);
        periodicReports.save(firstReport);
        submitHomeroom(testStudent1);

        String adminToken = loginAsAdmin();
        mockMvc.perform(get("/api/semester-results/admin/summary")
                        .header("Authorization", authHeader(adminToken))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString())
                        .param("classId", testClass.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].violationCount").value(1));

        mockMvc.perform(put("/api/semester-results/admin/students/{studentId}", testStudent1.getId())
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "academicYearId", testAcademicYear.getId(), "semesterId", testSemester.getId(),
                                "classId", testClass.getId(), "academicAbility", "Giỏi",
                                "conduct", "Khá", "honor", "Học sinh tiên tiến"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conduct").value("Khá"));

        mockMvc.perform(post("/api/semester-results/admin/publish")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scopeBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PUBLISHED"));

        mockMvc.perform(get("/api/periodic-reports/students/{studentId}", testStudent1.getId())
                        .header("Authorization", authHeader(loginAsParent()))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.conduct").value("Khá"))
                .andExpect(jsonPath("$.data.subjectReviews[0].comment").exists());

        SemesterResult published = semesterResults
                .findByStudentIdAndSemesterId(testStudent1.getId(), testSemester.getId()).orElseThrow();
        assertThat(published.getPublishedAt()).isNotNull();
        assertThat(published.getConductSource()).isEqualTo(ConductSource.ADMIN);
    }

    @Test
    void resultSummaryRejectsCrossYearScope() throws Exception {
        mockMvc.perform(get("/api/semester-results/admin/summary")
                        .header("Authorization", authHeader(loginAsAdmin()))
                        .param("academicYearId", "999999")
                        .param("semesterId", testSemester.getId().toString())
                        .param("classId", testClass.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void legacyHomeroomConductSourceIsReadAsAdmin() {
        jdbcTemplate.update("""
                INSERT INTO semester_results (
                    student_id, semester_id, class_id, conduct, suggested_conduct,
                    conduct_source, result_overridden
                ) VALUES (?, ?, ?, ?, ?, 'HOMEROOM', TRUE)
                """,
                testStudent1.getId(), testSemester.getId(), testClass.getId(), "Tốt", "Tốt");
        entityManager.clear();

        SemesterResult legacyResult = semesterResults
                .findByStudentIdAndSemesterId(testStudent1.getId(), testSemester.getId())
                .orElseThrow();

        assertThat(legacyResult.getConductSource()).isEqualTo(ConductSource.ADMIN);
        assertThat(legacyResult.getResultOverridden()).isTrue();
    }

    private void saveSubjectReview(Student student, String comment) throws Exception {
        mockMvc.perform(put("/api/subject-reviews/{studentId}", student.getId())
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjectReviewBody(student, comment)))
                .andExpect(status().isOk());
    }

    private String subjectReviewBody(Student student, String comment) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "academicYearId", testAcademicYear.getId(), "semesterId", testSemester.getId(),
                "classId", testClass.getId(), "subjectId", testSubject.getId(),
                "comment", comment, "strengths", "Điểm mạnh", "improvements", "Cần cải thiện"));
    }

    private void submitSubjectReviews(List<Long> studentIds) throws Exception {
        mockMvc.perform(post("/api/subject-reviews/submit")
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "academicYearId", testAcademicYear.getId(), "semesterId", testSemester.getId(),
                                "classId", testClass.getId(), "subjectId", testSubject.getId(),
                                "studentIds", studentIds))))
                .andExpect(status().isOk());
    }

    private void saveHomeroomDraft(Student student, String comment) throws Exception {
        mockMvc.perform(put("/api/homeroom-reports/students/{studentId}", student.getId())
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "academicYearId", testAcademicYear.getId(), "semesterId", testSemester.getId(),
                                "classId", testClass.getId(), "generalComment", comment))))
                .andExpect(status().isOk());
    }

    private void submitHomeroom(Student student) throws Exception {
        mockMvc.perform(post("/api/homeroom-reports/students/{studentId}/submit", student.getId())
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON).content(scopeBody()))
                .andExpect(status().isOk());
    }

    private void createViolation(Student student, String title) throws Exception {
        mockMvc.perform(post("/api/students/{studentId}/events", student.getId())
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "academicYearId", testAcademicYear.getId(), "semesterId", testSemester.getId(),
                                "classId", testClass.getId(), "eventType", "VIOLATION", "category", "Nề nếp",
                                "title", title, "description", "Ghi nhận nội bộ", "eventDate", testSemester.getStartDate()))))
                .andExpect(status().isOk());
    }

    private void createSemesterResult(Student student, int rank) {
        SemesterResult result = new SemesterResult();
        result.setStudent(student); result.setSemester(testSemester); result.setCls(testClass);
        result.setGpa(BigDecimal.valueOf(9 - rank * .2)); result.setRank(rank);
        result.setSuggestedAcademicAbility("Giỏi"); result.setAcademicAbility("Giỏi");
        result.setSuggestedConduct("Tốt"); result.setConduct("Tốt");
        result.setSuggestedHonor("Giỏi"); result.setHonor("Giỏi");
        result.setConductSource(ConductSource.SUGGESTED); result.setResultOverridden(false);
        semesterResults.save(result);
    }

    private String scopeBody() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "academicYearId", testAcademicYear.getId(), "semesterId", testSemester.getId(),
                "classId", testClass.getId()));
    }
}
