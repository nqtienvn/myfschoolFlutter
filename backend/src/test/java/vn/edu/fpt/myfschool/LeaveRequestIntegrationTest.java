package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.entity.Attendance;
import vn.edu.fpt.myfschool.entity.AttendanceDetail;
import vn.edu.fpt.myfschool.entity.AttendanceSession;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.AttendanceRepository;
import vn.edu.fpt.myfschool.repository.AttendanceDetailRepository;
import vn.edu.fpt.myfschool.repository.AttendanceSessionRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaveRequestIntegrationTest extends BaseIntegrationTest {

    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private AttendanceSessionRepository attendanceSessionRepository;
    @Autowired private AttendanceDetailRepository attendanceDetailRepository;

    @BeforeEach
    void assignHomeroomTeacher() {
        HomeroomAssignment assignment = new HomeroomAssignment();
        assignment.setCls(testClass);
        assignment.setTeacher(testTeacher);
        assignment.setAcademicYear(testAcademicYear);
        assignment.setEffectiveFrom(testAcademicYear.getStartDate());
        homeroomAssignmentRepository.save(assignment);
    }

    private String leaveJson(Long studentId, String dateFrom, String dateTo, String shift, String reason) {
        return "{\"studentId\":" + studentId + ",\"dateFrom\":\"" + dateFrom + "\",\"dateTo\":\"" + dateTo
            + "\",\"shift\":\"" + shift + "\",\"reason\":\"" + reason + "\"}";
    }

    private Semester createSecondSemester() {
        Semester semester = new Semester();
        semester.setName("HK II");
        semester.setAcademicYear(testAcademicYear);
        semester.setOrder(2);
        semester.setStartDate(java.time.LocalDate.of(2027, 1, 16));
        semester.setEndDate(java.time.LocalDate.of(2027, 5, 31));
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

    @Test
    void create_leave_request_parent_only() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-09-01", "2026-09-01", "FULL_DAY", "Con bi om")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.shift").value("FULL_DAY"));
    }

    @Test
    void creating_leave_request_notifies_homeroom_teacher_with_deep_link() throws Exception {
        String parentToken = loginAsParent();
        mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(parentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-09-18", "2026-09-18", "MORNING", "Kham benh")))
            .andExpect(status().isOk());

        String teacherToken = loginAsTeacher();
        mockMvc.perform(get("/api/notifications")
                .header("Authorization", authHeader(teacherToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].relatedType").value("LEAVE_REQUEST"))
            .andExpect(jsonPath("$.data[0].relatedId").isNumber());
    }

    @Test
    void approving_leave_does_not_turn_present_student_into_absent() throws Exception {
        String teacherToken = loginAsTeacher();
        java.time.LocalDate date = java.time.LocalDate.of(2026, 9, 19);
        Attendance attendance = new Attendance();
        attendance.setStudent(testStudent1);
        attendance.setCls(testClass);
        attendance.setTeacher(testTeacher);
        attendance.setDate(date);
        attendance.setShift(Shift.MORNING);
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendanceRepository.save(attendance);

        AttendanceSession session = new AttendanceSession();
        session.setCls(testClass);
        session.setTeacher(testTeacher);
        session.setDate(date);
        session.setShift(Shift.MORNING);
        session.setTotal(1);
        session.setPresent(0);
        session.setAbsent(1);
        session = attendanceSessionRepository.save(session);
        AttendanceDetail detail = new AttendanceDetail();
        detail.setSession(session);
        detail.setStudent(testStudent1);
        detail.setStatus(AttendanceStatus.ABSENT_WITHOUT_LEAVE);
        detail = attendanceDetailRepository.save(detail);

        String parentToken = loginAsParent();
        MvcResult created = mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(parentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-09-19", "2026-09-19", "MORNING", "Kham benh")))
            .andExpect(status().isOk())
            .andReturn();
        long leaveId = objectMapper.readTree(created.getResponse().getContentAsString())
            .path("data").path("id").asLong();

        mockMvc.perform(put("/api/leave-requests/" + leaveId + "/approve")
                .header("Authorization", authHeader(teacherToken)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/leave-requests/reviewed")
                .header("Authorization", authHeader(teacherToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.id == " + leaveId + ")].status")
                .value(hasItem("APPROVED")))
            .andExpect(jsonPath("$.data[?(@.id == " + leaveId + ")].academicYearId")
                .value(hasItem(testAcademicYear.getId().intValue())));

        mockMvc.perform(get("/api/attendance/daily")
                .header("Authorization", authHeader(teacherToken))
                .param("classId", testClass.getId().toString())
                .param("date", "2026-09-19")
                .param("shift", "MORNING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.students[?(@.studentId == " + testStudent1.getId() + ")].status")
                .value(hasItem("PRESENT")));

        assertEquals(AttendanceStatus.PRESENT,
            attendanceRepository.findById(attendance.getId()).orElseThrow().getStatus());
        assertEquals(AttendanceStatus.PRESENT,
            attendanceDetailRepository.findById(detail.getId()).orElseThrow().getStatus());
        assertEquals(1,
            attendanceSessionRepository.findById(session.getId()).orElseThrow().getPresent());
        assertEquals(0,
            attendanceSessionRepository.findById(session.getId()).orElseThrow().getAbsent());
    }

    @Test
    void approving_leave_keeps_canonical_and_session_attendance_in_sync() throws Exception {
        java.time.LocalDate date = java.time.LocalDate.of(2026, 9, 21);
        Attendance attendance = new Attendance();
        attendance.setStudent(testStudent1);
        attendance.setCls(testClass);
        attendance.setTeacher(testTeacher);
        attendance.setDate(date);
        attendance.setShift(Shift.MORNING);
        attendance.setStatus(AttendanceStatus.ABSENT_WITHOUT_LEAVE);
        attendanceRepository.save(attendance);

        AttendanceSession session = new AttendanceSession();
        session.setCls(testClass);
        session.setTeacher(testTeacher);
        session.setDate(date);
        session.setShift(Shift.MORNING);
        session.setTotal(1);
        session.setPresent(1);
        session.setAbsent(0);
        session = attendanceSessionRepository.save(session);
        AttendanceDetail detail = new AttendanceDetail();
        detail.setSession(session);
        detail.setStudent(testStudent1);
        // Deliberately stale session data: canonical attendance is the source of truth.
        detail.setStatus(AttendanceStatus.PRESENT);
        detail = attendanceDetailRepository.save(detail);

        String parentToken = loginAsParent();
        MvcResult created = mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(parentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-09-21", "2026-09-21", "MORNING", "Kham benh")))
            .andExpect(status().isOk())
            .andReturn();
        long leaveId = objectMapper.readTree(created.getResponse().getContentAsString())
            .path("data").path("id").asLong();

        mockMvc.perform(put("/api/leave-requests/" + leaveId + "/approve")
                .header("Authorization", authHeader(loginAsTeacher())))
            .andExpect(status().isOk());

        assertEquals(AttendanceStatus.ABSENT_WITH_LEAVE,
            attendanceRepository.findById(attendance.getId()).orElseThrow().getStatus());
        assertEquals(AttendanceStatus.ABSENT_WITH_LEAVE,
            attendanceDetailRepository.findById(detail.getId()).orElseThrow().getStatus());
        assertEquals(0,
            attendanceSessionRepository.findById(session.getId()).orElseThrow().getPresent());
        assertEquals(1,
            attendanceSessionRepository.findById(session.getId()).orElseThrow().getAbsent());
    }

    @Test
    void approving_leave_creates_missing_session_detail_from_canonical_attendance()
            throws Exception {
        java.time.LocalDate date = java.time.LocalDate.of(2026, 9, 22);
        Attendance attendance = new Attendance();
        attendance.setStudent(testStudent1);
        attendance.setCls(testClass);
        attendance.setTeacher(testTeacher);
        attendance.setDate(date);
        attendance.setShift(Shift.MORNING);
        attendance.setStatus(AttendanceStatus.ABSENT_WITHOUT_LEAVE);
        attendance = attendanceRepository.save(attendance);

        AttendanceSession session = new AttendanceSession();
        session.setCls(testClass);
        session.setTeacher(testTeacher);
        session.setDate(date);
        session.setShift(Shift.MORNING);
        session.setTotal(0);
        session.setPresent(0);
        session.setAbsent(0);
        session = attendanceSessionRepository.save(session);

        MvcResult created = mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(loginAsParent()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-09-22", "2026-09-22",
                    "MORNING", "Kham benh")))
            .andExpect(status().isOk())
            .andReturn();
        long leaveId = objectMapper.readTree(created.getResponse().getContentAsString())
            .path("data").path("id").asLong();

        mockMvc.perform(put("/api/leave-requests/" + leaveId + "/approve")
                .header("Authorization", authHeader(loginAsTeacher())))
            .andExpect(status().isOk());

        assertEquals(AttendanceStatus.ABSENT_WITH_LEAVE,
            attendanceRepository.findById(attendance.getId()).orElseThrow().getStatus());
        assertEquals(AttendanceStatus.ABSENT_WITH_LEAVE,
            attendanceDetailRepository.findBySessionIdAndStudentId(
                session.getId(), testStudent1.getId()).orElseThrow().getStatus());
        assertEquals(1,
            attendanceSessionRepository.findById(session.getId()).orElseThrow().getTotal());
        assertEquals(0,
            attendanceSessionRepository.findById(session.getId()).orElseThrow().getPresent());
        assertEquals(1,
            attendanceSessionRepository.findById(session.getId()).orElseThrow().getAbsent());
    }

    @Test
    void create_leave_request_student_forbidden() throws Exception {
        String token = loginAsStudent1();

        mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-09-02", "2026-09-02", "MORNING", "Test")))
            .andExpect(status().isForbidden());
    }

    @Test
    void create_leave_request_empty_reason_fails() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-09-04", "2026-09-04", "FULL_DAY", "")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void create_leave_request_date_from_after_date_to_fails() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-09-10", "2026-09-05", "FULL_DAY", "Ngay sai")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void parent_view_my_leave_requests() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-09-11", "2026-09-11", "MORNING", "Don 1")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/leave-requests/my").header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void teacher_view_pending_requests() throws Exception {
        String token = loginAsTeacher();

        mockMvc.perform(get("/api/leave-requests/pending").header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void teacher_view_pending_count() throws Exception {
        String token = loginAsTeacher();

        mockMvc.perform(get("/api/leave-requests/pending-count").header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    void teacher_leave_endpoints_filter_by_selected_semester() throws Exception {
        Semester secondSemester = createSecondSemester();
        String parentToken = loginAsParent();
        MvcResult firstCreated = mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(parentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-09-20", "2026-09-20", "FULL_DAY", "HK I")))
            .andExpect(status().isOk())
            .andReturn();
        long firstId = objectMapper.readTree(firstCreated.getResponse().getContentAsString())
            .path("data").path("id").asLong();
        MvcResult secondCreated = mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(parentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2027-02-20", "2027-02-20", "FULL_DAY", "HK II")))
            .andExpect(status().isOk())
            .andReturn();
        long secondId = objectMapper.readTree(secondCreated.getResponse().getContentAsString())
            .path("data").path("id").asLong();

        String teacherToken = loginAsTeacher();
        mockMvc.perform(get("/api/leave-requests/pending")
                .header("Authorization", authHeader(teacherToken))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value(firstId));
        mockMvc.perform(get("/api/leave-requests/pending-count")
                .header("Authorization", authHeader(teacherToken))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(1));

        mockMvc.perform(put("/api/leave-requests/" + firstId + "/approve")
                .header("Authorization", authHeader(teacherToken)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/leave-requests/reviewed")
                .header("Authorization", authHeader(teacherToken))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value(firstId));
        mockMvc.perform(get("/api/leave-requests/pending")
                .header("Authorization", authHeader(teacherToken))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", secondSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value(secondId));
    }

    @Test
    void homeroomHandover_limitsLeaveVisibilityCountAndReviewToAssignmentWindow()
            throws Exception {
        java.time.LocalDate handoverDate = java.time.LocalDate.of(2026, 10, 16);
        HomeroomAssignment firstAssignment = homeroomAssignmentRepository
            .findByTeacherIdAndAcademicYearId(testTeacher.getId(), testAcademicYear.getId())
            .get(0);
        firstAssignment.setEffectiveTo(handoverDate.minusDays(1));
        homeroomAssignmentRepository.save(firstAssignment);

        Teacher nextTeacher = createTeacher(
            "0909000092", "GV-HANDOVER", "GV Nhan Ban Giao");
        HomeroomAssignment nextAssignment = new HomeroomAssignment();
        nextAssignment.setCls(testClass);
        nextAssignment.setTeacher(nextTeacher);
        nextAssignment.setAcademicYear(testAcademicYear);
        nextAssignment.setEffectiveFrom(handoverDate);
        homeroomAssignmentRepository.save(nextAssignment);

        String parentToken = loginAsParent();
        MvcResult earlyCreated = mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(parentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-09-20", "2026-09-20", "FULL_DAY", "Truoc ban giao")))
            .andExpect(status().isOk())
            .andReturn();
        long earlyId = objectMapper.readTree(earlyCreated.getResponse().getContentAsString())
            .path("data").path("id").asLong();
        MvcResult lateCreated = mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(parentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-11-20", "2026-11-20", "FULL_DAY", "Sau ban giao")))
            .andExpect(status().isOk())
            .andReturn();
        long lateId = objectMapper.readTree(lateCreated.getResponse().getContentAsString())
            .path("data").path("id").asLong();

        String firstTeacherToken = loginAsTeacher();
        String nextTeacherToken = login("0909000092", "test1234");
        mockMvc.perform(get("/api/leave-requests/pending")
                .header("Authorization", authHeader(firstTeacherToken))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value(earlyId));
        mockMvc.perform(get("/api/leave-requests/pending-count")
                .header("Authorization", authHeader(firstTeacherToken))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(1));
        mockMvc.perform(get("/api/leave-requests/pending")
                .header("Authorization", authHeader(nextTeacherToken))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value(lateId));

        mockMvc.perform(put("/api/leave-requests/" + lateId + "/approve")
                .header("Authorization", authHeader(firstTeacherToken)))
            .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/leave-requests/" + earlyId + "/approve")
                .header("Authorization", authHeader(nextTeacherToken)))
            .andExpect(status().isForbidden());
    }

    @Test
    void create_leave_request_rejects_range_spanning_homeroom_handover()
            throws Exception {
        java.time.LocalDate handoverDate = java.time.LocalDate.of(2026, 10, 16);
        HomeroomAssignment firstAssignment = homeroomAssignmentRepository
            .findByTeacherIdAndAcademicYearId(testTeacher.getId(), testAcademicYear.getId())
            .get(0);
        firstAssignment.setEffectiveTo(handoverDate.minusDays(1));
        homeroomAssignmentRepository.save(firstAssignment);

        Teacher nextTeacher = createTeacher(
            "0909000093", "GV-HANDOVER-RANGE", "GV Nhan Ban Giao Range");
        HomeroomAssignment nextAssignment = new HomeroomAssignment();
        nextAssignment.setCls(testClass);
        nextAssignment.setTeacher(nextTeacher);
        nextAssignment.setAcademicYear(testAcademicYear);
        nextAssignment.setEffectiveFrom(handoverDate);
        homeroomAssignmentRepository.save(nextAssignment);

        mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(loginAsParent()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-10-15", "2026-10-16",
                    "FULL_DAY", "Qua ngay ban giao")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void teacher_leave_scope_rejects_mismatched_year_and_semester() throws Exception {
        String token = loginAsTeacher();

        mockMvc.perform(get("/api/leave-requests/pending")
                .header("Authorization", authHeader(token))
                .param("academicYearId", String.valueOf(testAcademicYear.getId() + 1))
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void teacher_leave_scope_requires_assignment_effective_in_selected_semester() throws Exception {
        Semester secondSemester = createSecondSemester();
        HomeroomAssignment assignment = homeroomAssignmentRepository
            .findByTeacherIdAndAcademicYearId(testTeacher.getId(), testAcademicYear.getId())
            .get(0);
        assignment.setEffectiveTo(testSemester.getEndDate());
        homeroomAssignmentRepository.save(assignment);
        String token = loginAsTeacher();

        mockMvc.perform(get("/api/leave-requests/pending")
                .header("Authorization", authHeader(token))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", secondSemester.getId().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    void non_homeroom_teacher_cannot_access_leave_review_endpoints() throws Exception {
        homeroomAssignmentRepository.deleteAll();
        String token = loginAsTeacher();

        mockMvc.perform(get("/api/leave-requests/pending")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/leave-requests/pending-count")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isForbidden());
    }

    @Test
    void create_overlapping_pending_request_fails() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-10-20", "2026-10-22", "FULL_DAY", "Don dau")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-10-21", "2026-10-21", "MORNING", "Don trung")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }
}
