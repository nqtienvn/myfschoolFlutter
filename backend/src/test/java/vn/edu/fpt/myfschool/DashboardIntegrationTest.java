package vn.edu.fpt.myfschool;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.enums.TargetRole;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.entity.Announcement;
import vn.edu.fpt.myfschool.entity.AnnouncementClass;
import vn.edu.fpt.myfschool.entity.AnnouncementRead;
import vn.edu.fpt.myfschool.entity.Attendance;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.SemesterResult;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.AnnouncementClassRepository;
import vn.edu.fpt.myfschool.repository.AnnouncementReadRepository;
import vn.edu.fpt.myfschool.repository.AnnouncementRepository;
import vn.edu.fpt.myfschool.repository.AttendanceRepository;
import vn.edu.fpt.myfschool.repository.SemesterResultRepository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardIntegrationTest extends BaseIntegrationTest {

    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private SemesterResultRepository semesterResultRepository;
    @Autowired private AnnouncementRepository announcementRepository;
    @Autowired private AnnouncementClassRepository announcementClassRepository;
    @Autowired private AnnouncementReadRepository announcementReadRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    @BeforeEach
    void setUpDashboardData() {
        HomeroomAssignment homeroom = new HomeroomAssignment();
        homeroom.setCls(testClass);
        homeroom.setTeacher(testTeacher);
        homeroom.setAcademicYear(testAcademicYear);
        homeroom.setEffectiveFrom(testAcademicYear.getStartDate());
        homeroomAssignmentRepository.save(homeroom);

        saveAttendance(Shift.MORNING, AttendanceStatus.PRESENT);
        saveAttendance(Shift.AFTERNOON, AttendanceStatus.ABSENT_WITHOUT_LEAVE);
        saveSemesterResult(testStudent1, new BigDecimal("8.00"), 1);
        saveSemesterResult(testStudent2, new BigDecimal("6.00"), 2);

        Announcement first = saveParentAnnouncement("Thong bao 1");
        Announcement second = saveParentAnnouncement("Thong bao 2");
        moveAnnouncementTo(first, testSemester.getStartDate().plusDays(1).atTime(8, 0));
        moveAnnouncementTo(second, testSemester.getStartDate().plusDays(2).atTime(8, 0));
        AnnouncementRead read = new AnnouncementRead();
        read.setAnnouncement(first);
        read.setUser(testParent.getUser());
        read.setReadAt(LocalDateTime.now());
        announcementReadRepository.save(read);
    }

    @Test
    void teacherDashboard_returnsPeriodScopedHomeroomMetrics() throws Exception {
        String token = loginAsTeacher();

        mockMvc.perform(get("/api/dashboard/teacher")
                .header("Authorization", authHeader(token))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.classId").value(testClass.getId()))
            .andExpect(jsonPath("$.data.className").value("12A"))
            .andExpect(jsonPath("$.data.academicYearId").value(testAcademicYear.getId()))
            .andExpect(jsonPath("$.data.semesterId").value(testSemester.getId()))
            .andExpect(jsonPath("$.data.attendanceRate").value(50.0))
            .andExpect(jsonPath("$.data.averageGpa").value(7.0))
            .andExpect(jsonPath("$.data.parentReadRate").value(50.0));
    }

    @Test
    void teacherDashboard_usesAssignmentEffectiveOnSemesterReferenceDate() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate referenceDate = !today.isBefore(testSemester.getStartDate())
                && !today.isAfter(testSemester.getEndDate())
            ? today : testSemester.getStartDate();
        Teacher handoverTeacher = createTeacher(
            "0909000091", "GV-HANDOVER", "GV Ban Giao");
        HomeroomAssignment handover = new HomeroomAssignment();
        handover.setCls(testClass);
        handover.setTeacher(handoverTeacher);
        handover.setAcademicYear(testAcademicYear);
        if (referenceDate.isBefore(testSemester.getEndDate())) {
            handover.setEffectiveFrom(referenceDate.plusDays(1));
        } else {
            handover.setEffectiveFrom(testSemester.getStartDate());
            handover.setEffectiveTo(referenceDate.minusDays(1));
        }
        homeroomAssignmentRepository.save(handover);

        mockMvc.perform(get("/api/dashboard/teacher")
                .header("Authorization", authHeader(login("0909000091", "test1234")))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/dashboard/student")
                .header("Authorization", authHeader(loginAsStudent1()))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.homeroomTeacherName").value("GV Test"));
    }

    @Test
    void teacherDashboard_excludesOtherSemesterAnnouncementsAndReturnsNullWithoutDenominator()
            throws Exception {
        Semester secondSemester = createSecondSemester();

        mockMvc.perform(get("/api/dashboard/teacher")
                .header("Authorization", authHeader(loginAsTeacher()))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", secondSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.semesterId").value(secondSemester.getId()))
            .andExpect(jsonPath("$.data.attendanceRate").value(nullValue()))
            .andExpect(jsonPath("$.data.parentReadRate").value(nullValue()));
    }

    @Test
    void studentDashboard_mapsHomeroomTeacherForSelectedPeriod() throws Exception {
        String token = loginAsStudent1();

        mockMvc.perform(get("/api/dashboard/student")
                .header("Authorization", authHeader(token))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.classId").value(testClass.getId()))
            .andExpect(jsonPath("$.data.homeroomTeacherName").value("GV Test"))
            .andExpect(jsonPath("$.data.homeroomTeacherPhone").value("0909000001"));
    }

    @Test
    void parentDashboard_returnsLinkedStudentForSelectedPeriod() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(get("/api/dashboard/student")
                .header("Authorization", authHeader(token))
                .param("studentId", testStudent1.getId().toString())
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.studentId").value(testStudent1.getId()))
            .andExpect(jsonPath("$.data.classId").value(testClass.getId()))
            .andExpect(jsonPath("$.data.academicYearId").value(testAcademicYear.getId()))
            .andExpect(jsonPath("$.data.semesterId").value(testSemester.getId()))
            .andExpect(jsonPath("$.data.attendanceRate").value(50.0))
            .andExpect(jsonPath("$.data.currentGpa").value(8.0))
            .andExpect(jsonPath("$.data.homeroomTeacherName").value("GV Test"));
    }

    @Test
    void parentDashboard_rejectsStudentOutsideGuardianLinks() throws Exception {
        studentGuardianRepository.delete(
            studentGuardianRepository.findByStudentIdAndGuardianId(
                testStudent3.getId(), testParent.getId()).orElseThrow());
        studentGuardianRepository.flush();

        mockMvc.perform(get("/api/dashboard/student")
                .header("Authorization", authHeader(loginAsParent()))
                .param("studentId", testStudent3.getId().toString())
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    void studentDashboard_rejectsAnotherStudentId() throws Exception {
        mockMvc.perform(get("/api/dashboard/student")
                .header("Authorization", authHeader(loginAsStudent1()))
                .param("studentId", testStudent2.getId().toString())
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    void parentLoginProfile_mapsCurrentClassHomeroomTeacher() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0909000002\",\"password\":\"test1234\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.parentProfile.children[0].homeroomTeacherName")
                .value("GV Test"))
            .andExpect(jsonPath("$.data.user.parentProfile.children[0].homeroomTeacherPhone")
                .value("0909000001"));
    }

    @Test
    void studentCannotAccessTeacherDashboard() throws Exception {
        String token = loginAsStudent1();

        mockMvc.perform(get("/api/dashboard/teacher")
                .header("Authorization", authHeader(token))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isForbidden());
    }

    private void saveAttendance(Shift shift, AttendanceStatus status) {
        Attendance attendance = new Attendance();
        attendance.setStudent(testStudent1);
        attendance.setCls(testClass);
        attendance.setTeacher(testTeacher);
        attendance.setDate(testSemester.getStartDate().plusDays(1));
        attendance.setShift(shift);
        attendance.setStatus(status);
        attendanceRepository.save(attendance);
    }

    private void saveSemesterResult(
            vn.edu.fpt.myfschool.entity.Student student,
            BigDecimal gpa,
            int rank) {
        SemesterResult result = new SemesterResult();
        result.setStudent(student);
        result.setSemester(testSemester);
        result.setCls(testClass);
        result.setGpa(gpa);
        result.setRank(rank);
        semesterResultRepository.save(result);
    }

    private Announcement saveParentAnnouncement(String title) {
        Announcement announcement = new Announcement();
        announcement.setTeacher(testTeacher);
        announcement.setSender(testTeacher.getUser());
        announcement.setAcademicYear(testAcademicYear);
        announcement.setApprovalStatus("APPROVED");
        announcement.setSenderType("HOMEROOM_TEACHER");
        announcement.setRecipientScope("CLASSES");
        announcement.setTitle(title);
        announcement.setBody(title);
        announcement.setTargetRole(TargetRole.PARENT);
        announcement = announcementRepository.save(announcement);

        AnnouncementClass link = new AnnouncementClass();
        link.setAnnouncement(announcement);
        link.setCls(testClass);
        announcementClassRepository.save(link);
        return announcement;
    }

    private Semester createSecondSemester() {
        Semester semester = new Semester();
        semester.setName("HK II");
        semester.setAcademicYear(testAcademicYear);
        semester.setOrder(2);
        semester.setStartDate(LocalDate.of(2027, 1, 16));
        semester.setEndDate(LocalDate.of(2027, 5, 31));
        semester.setIsCurrent(false);
        return semesterRepository.save(semester);
    }

    private Teacher createTeacher(String phone, String employeeCode, String name) {
        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("test1234"));
        user.setName(name);
        user.setRole(UserRole.TEACHER);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setEmployeeCode(employeeCode);
        return teacherRepository.save(teacher);
    }

    private void moveAnnouncementTo(Announcement announcement, LocalDateTime createdAt) {
        announcementRepository.flush();
        jdbcTemplate.update(
            "UPDATE announcements SET created_at = ? WHERE id = ?",
            Timestamp.valueOf(createdAt), announcement.getId());
        entityManager.refresh(announcement);
    }
}
