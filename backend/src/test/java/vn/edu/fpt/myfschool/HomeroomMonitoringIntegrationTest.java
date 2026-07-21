package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.enums.*;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HomeroomMonitoringIntegrationTest extends BaseIntegrationTest {
    @Autowired private StudentRiskFlagRepository riskFlags;
    @Autowired private SemesterResultRepository semesterResults;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private ParentContactLogRepository contactLogs;
    @Autowired private ParentMeetingRepository meetings;
    @Autowired private ParentMeetingParticipantRepository participants;
    @Autowired private NotificationRepository notifications;

    private String teacherToken;
    private HomeroomAssignment homeroom;

    @BeforeEach
    void setUpHomeroom() throws Exception {
        teacherToken = loginAsTeacher();
        homeroom = new HomeroomAssignment();
        homeroom.setAcademicYear(testAcademicYear);
        homeroom.setCls(testClass);
        homeroom.setTeacher(testTeacher);
        homeroom.setEffectiveFrom(LocalDate.now().minusDays(1));
        homeroom = homeroomAssignmentRepository.save(homeroom);
    }

    @Test
    void riskRecalculationIsIdempotentAndResolvesWhenSourcesRecover() throws Exception {
        configureRisks(80, 0);
        SemesterResult result = semesterResult(testStudent1, BigDecimal.valueOf(4));
        Attendance present = attendance(testStudent1, LocalDate.of(2026, 9, 2), AttendanceStatus.PRESENT);
        Attendance absent = attendance(testStudent1, LocalDate.of(2026, 9, 3), AttendanceStatus.ABSENT_WITHOUT_LEAVE);

        recalculateRisks().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
        recalculateRisks().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
        assertThat(riskFlags.count()).isEqualTo(3);

        result.setGpa(BigDecimal.valueOf(8));
        semesterResults.save(result);
        absent.setStatus(AttendanceStatus.PRESENT);
        attendanceRepository.save(absent);
        assertThat(present.getId()).isNotNull();

        recalculateRisks().andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("RESOLVED"));
        assertThat(riskFlags.findAll()).allMatch(item -> item.getStatus() == RiskStatus.RESOLVED);
    }

    @Test
    void onlyHomeroomTeacherCanSeeRisksAndInternalContactLogs() throws Exception {
        Teacher other = createTeacher("0909888001", "GV Bộ môn", "GV888");
        TeachingAssignment assignment = new TeachingAssignment();
        assignment.setCls(testClass); assignment.setSubject(subjectRepository.findByCode("VAN12").orElseThrow());
        assignment.setTeacher(other); assignment.setEffectiveFrom(testSemester.getStartDate());
        assignment.setStatus(AssignmentStatus.ACTIVE); teachingAssignmentRepository.save(assignment);
        String otherToken = login("0909888001", "test1234");

        mockMvc.perform(get("/api/homeroom/risks")
                        .header("Authorization", authHeader(otherToken))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString())
                        .param("classId", testClass.getId().toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/homeroom/risks")
                        .header("Authorization", authHeader(loginAsParent()))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString())
                        .param("classId", testClass.getId().toString()))
                .andExpect(status().isForbidden());

        String body = contactBody("Trao đổi kết quả học tập");
        mockMvc.perform(post("/api/students/{studentId}/contact-logs", testStudent1.getId())
                        .header("Authorization", authHeader(otherToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/students/{studentId}/contact-logs", testStudent1.getId())
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classId").value(testClass.getId()));
    }

    @Test
    void parentOnlyRespondsToOwnMeetingAndReceivesRealtimeBackedNotification() throws Exception {
        String meetingBody = objectMapper.writeValueAsString(Map.of(
                "title", "Họp phụ huynh giữa kỳ", "academicYearId", testAcademicYear.getId(),
                "semesterId", testSemester.getId(), "classId", testClass.getId(),
                "studentId", testStudent1.getId(), "startsAt", "2026-10-20T18:30:00",
                "location", "Phòng 201", "agenda", "Trao đổi tiến độ học tập"));
        mockMvc.perform(post("/api/parent-meetings")
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON).content(meetingBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participants.length()").value(1));
        ParentMeeting meeting = meetings.findAll().getFirst();
        assertThat(notifications.findByUserIdOrderByCreatedAtDesc(testParent.getUser().getId()))
                .anyMatch(item -> "PARENT_MEETING".equals(item.getRelatedType()));

        String parentToken = loginAsParent();
        mockMvc.perform(put("/api/parent-meetings/{id}/respond", meeting.getId())
                        .header("Authorization", authHeader(parentToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"response\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participants[0].response").value("ACCEPTED"));

        Parent outsider = createParent("0909888002", "PH Ngoài lớp");
        mockMvc.perform(put("/api/parent-meetings/{id}/respond", meeting.getId())
                        .header("Authorization", authHeader(login("0909888002", "test1234")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"response\":\"DECLINED\"}"))
                .andExpect(status().isForbidden());
        assertThat(outsider.getId()).isNotNull();
    }

    @Test
    void classSummaryUsesCanonicalSourcesAndRejectsAnotherYear() throws Exception {
        semesterResult(testStudent1, BigDecimal.valueOf(8));
        attendance(testStudent1, LocalDate.of(2026, 9, 2), AttendanceStatus.PRESENT);
        attendance(testStudent2, LocalDate.of(2026, 9, 2), AttendanceStatus.ABSENT_WITHOUT_LEAVE);

        StudentRiskFlag risk = new StudentRiskFlag();
        risk.setAcademicYear(testAcademicYear); risk.setSemester(testSemester); risk.setCls(testClass);
        risk.setStudent(testStudent2); risk.setRiskType(RiskType.LOW_ATTENDANCE); risk.setSeverity(RiskSeverity.HIGH);
        risk.setMetricValue("0"); risk.setThresholdValue("80"); risk.setMessage("Chuyên cần thấp");
        risk.setDetectedAt(LocalDateTime.now()); risk.setSourceSnapshotJson("{}"); riskFlags.save(risk);

        ParentContactLog log = new ParentContactLog();
        log.setStudent(testStudent1); log.setAcademicYear(testAcademicYear); log.setSemester(testSemester);
        log.setCls(testClass); log.setContactType(ParentContactType.CALL); log.setSubject("Trao đổi");
        log.setSummary("Đã liên hệ"); log.setContactedAt(LocalDateTime.now()); log.setCreatedBy(testTeacher.getUser());
        contactLogs.save(log);

        mockMvc.perform(get("/api/homeroom/reports/class-summary")
                        .header("Authorization", authHeader(loginAsAdmin()))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString())
                        .param("classId", testClass.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].studentCount").value(3))
                .andExpect(jsonPath("$.data[0].attendanceRate").value(50.0))
                .andExpect(jsonPath("$.data[0].averageGpa").value(8.0))
                .andExpect(jsonPath("$.data[0].openRiskCount").value(1))
                .andExpect(jsonPath("$.data[0].parentContactCount").value(1));

        AcademicYear otherYear = otherYear();
        Semester otherSemester = otherSemester(otherYear);
        mockMvc.perform(get("/api/homeroom/reports/class-summary")
                        .header("Authorization", authHeader(loginAsAdmin()))
                        .param("academicYearId", otherYear.getId().toString())
                        .param("semesterId", otherSemester.getId().toString())
                        .param("classId", testClass.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void transferredStudentKeepsOldContactHistoryInOriginalClass() throws Exception {
        mockMvc.perform(post("/api/students/{studentId}/contact-logs", testStudent1.getId())
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON).content(contactBody("Trước khi chuyển lớp")))
                .andExpect(status().isOk());
        Enrollment old = enrollmentRepository.findByStudentIdAndAcademicYearIdAndStatus(
                testStudent1.getId(), testAcademicYear.getId(), EnrollmentStatus.ACTIVE).orElseThrow();
        old.setStatus(EnrollmentStatus.TRANSFERRED); old.setLeaveDate(LocalDate.now()); enrollmentRepository.save(old);

        mockMvc.perform(get("/api/students/{studentId}/contact-logs", testStudent1.getId())
                        .header("Authorization", authHeader(teacherToken))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString())
                        .param("classId", testClass.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].classId").value(testClass.getId()));
    }

    private org.springframework.test.web.servlet.ResultActions recalculateRisks() throws Exception {
        return mockMvc.perform(post("/api/homeroom/risks/recalculate")
                .header("Authorization", authHeader(teacherToken))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", testSemester.getId().toString())
                .param("classId", testClass.getId().toString()));
    }

    private void configureRisks(int attendanceRate, int absences) throws Exception {
        mockMvc.perform(put("/api/homeroom/risk-config")
                        .header("Authorization", authHeader(loginAsAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.ofEntries(
                                Map.entry("academicYearId", testAcademicYear.getId()), Map.entry("minGpa", 5),
                                Map.entry("minAttendanceRate", attendanceRate), Map.entry("maxUnexcusedAbsences", absences),
                                Map.entry("conductRiskValues", "Yếu"), Map.entry("includeOverdueTuition", false),
                                Map.entry("overdueTuitionDays", 0), Map.entry("gpaSeverity", "HIGH"),
                                Map.entry("attendanceSeverity", "HIGH"), Map.entry("absenceSeverity", "HIGH"),
                                Map.entry("conductSeverity", "MEDIUM"), Map.entry("tuitionSeverity", "MEDIUM")))))
                .andExpect(status().isOk());
    }

    private SemesterResult semesterResult(Student student, BigDecimal gpa) {
        SemesterResult result = new SemesterResult(); result.setStudent(student); result.setSemester(testSemester);
        result.setCls(testClass); result.setGpa(gpa); result.setRank(1); result.setAcademicAbility("Khá");
        result.setConduct("Tốt"); result.setSuggestedConduct("Tốt"); return semesterResults.save(result);
    }

    private Attendance attendance(Student student, LocalDate date, AttendanceStatus status) {
        Attendance value = new Attendance(); value.setStudent(student); value.setCls(testClass);
        value.setTeacher(testTeacher); value.setDate(date); value.setShift(Shift.MORNING); value.setStatus(status);
        return attendanceRepository.save(value);
    }

    private String contactBody(String subject) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "academicYearId", testAcademicYear.getId(), "semesterId", testSemester.getId(),
                "classId", testClass.getId(), "contactType", "CALL", "subject", subject,
                "summary", "Phụ huynh đã tiếp nhận thông tin", "result", "Thống nhất phối hợp",
                "contactedAt", "2026-10-02T19:00:00"));
    }

    private Teacher createTeacher(String phone, String name, String code) {
        User user = new User(); user.setPhone(phone); user.setPassword(passwordEncoder.encode("test1234"));
        user.setName(name); user.setRole(UserRole.TEACHER); user.setStatus(UserStatus.ACTIVE); user = userRepository.save(user);
        Teacher teacher = new Teacher(); teacher.setUser(user); teacher.setEmployeeCode(code); return teacherRepository.save(teacher);
    }

    private Parent createParent(String phone, String name) {
        User user = new User(); user.setPhone(phone); user.setPassword(passwordEncoder.encode("test1234"));
        user.setName(name); user.setRole(UserRole.PARENT); user.setStatus(UserStatus.ACTIVE); user = userRepository.save(user);
        Parent parent = new Parent(); parent.setUser(user); return parentRepository.save(parent);
    }

    private AcademicYear otherYear() {
        AcademicYear year = new AcademicYear(); year.setName("2027-2028");
        year.setStartDate(LocalDate.of(2027, 8, 1)); year.setEndDate(LocalDate.of(2028, 5, 31));
        year.setStatus(AcademicYearStatus.DRAFT); return academicYearRepository.save(year);
    }

    private Semester otherSemester(AcademicYear year) {
        Semester semester = new Semester(); semester.setName("HK I"); semester.setOrder(1);
        semester.setAcademicYear(year); semester.setStartDate(LocalDate.of(2027, 9, 1));
        semester.setEndDate(LocalDate.of(2028, 1, 15)); return semesterRepository.save(semester);
    }
}
