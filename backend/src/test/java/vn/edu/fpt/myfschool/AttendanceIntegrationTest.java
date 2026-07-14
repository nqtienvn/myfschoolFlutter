package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.entity.LeaveRequest;
import vn.edu.fpt.myfschool.entity.Schedule;
import vn.edu.fpt.myfschool.entity.Timetable;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.LeaveShift;
import vn.edu.fpt.myfschool.common.enums.LeaveStatus;
import vn.edu.fpt.myfschool.common.enums.TimetableStatus;
import vn.edu.fpt.myfschool.repository.ScheduleRepository;
import vn.edu.fpt.myfschool.repository.TimetableRepository;
import vn.edu.fpt.myfschool.repository.AttendanceDetailRepository;
import vn.edu.fpt.myfschool.repository.AttendanceRepository;
import vn.edu.fpt.myfschool.repository.AttendanceSessionRepository;
import vn.edu.fpt.myfschool.repository.LeaveRequestRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AttendanceIntegrationTest extends BaseIntegrationTest {

    @Autowired private TimetableRepository timetableRepository;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private AttendanceSessionRepository attendanceSessionRepository;
    @Autowired private AttendanceDetailRepository attendanceDetailRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;

    @BeforeEach
    void assignHomeroomTeacher() {
        HomeroomAssignment assignment = new HomeroomAssignment();
        assignment.setCls(testClass);
        assignment.setTeacher(testTeacher);
        assignment.setAcademicYear(testAcademicYear);
        assignment.setEffectiveFrom(testAcademicYear.getStartDate());
        homeroomAssignmentRepository.save(assignment);

        Timetable timetable = new Timetable();
        timetable.setCls(testClass);
        timetable.setSemester(testSemester);
        timetable.setVersion(1);
        timetable.setStatus(TimetableStatus.ACTIVE);
        timetable.setEffectiveFrom(testSemester.getStartDate());
        timetable = timetableRepository.save(timetable);
        for (int day = 1; day <= 7; day++) {
            createSchedule(timetable, day, 1, Shift.MORNING, 0);
            createSchedule(timetable, day, 6, Shift.AFTERNOON, 5);
        }
    }

    private void createSchedule(
            Timetable timetable, int day, int period, Shift shift, int periodIndex) {
        Schedule schedule = new Schedule();
        schedule.setTimetable(timetable);
        schedule.setAssignment(testTeachingAssignment);
        schedule.setDayOfWeek(day);
        schedule.setPeriod(period);
        schedule.setPeriodRef(testPeriods.get(periodIndex));
        schedule.setShift(shift);
        scheduleRepository.save(schedule);
    }

    private LeaveRequest createApprovedLeave(
            java.time.LocalDate date, LeaveShift shift) {
        LeaveRequest leave = new LeaveRequest();
        leave.setStudent(testStudent1);
        leave.setParent(testParent);
        leave.setCls(testClass);
        leave.setAcademicYear(testAcademicYear);
        leave.setApprovedBy(testTeacher);
        leave.setDateFrom(date);
        leave.setDateTo(date);
        leave.setShift(shift);
        leave.setReason("Đơn nghỉ đã duyệt cho kiểm thử điểm danh");
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedAt(java.time.LocalDateTime.of(date, java.time.LocalTime.NOON));
        return leaveRequestRepository.save(leave);
    }

    private String attendanceJson(String shift, String entries) {
        return "{\"classId\":" + testClass.getId() + ",\"date\":\"2026-09-24\",\"shift\":\"" + shift + "\",\"entries\":[" + entries + "]}";
    }

    private String entry(Long studentId, String status) {
        return "{\"studentId\":" + studentId + ",\"status\":\"" + status + "\"}";
    }

    @Test
    void get_daily_attendance_teacher_only() throws Exception {
        String token = loginAsTeacher();

        mockMvc.perform(get("/api/attendance/daily")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("date", "2026-09-24")
                .param("shift", "MORNING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.classId").value(testClass.getId().intValue()))
            .andExpect(jsonPath("$.data.shift").value("MORNING"))
            .andExpect(jsonPath("$.data.students").isArray());
    }

    @Test
    void get_homeroom_context_uses_assignment_and_year_shifts() throws Exception {
        String token = loginAsTeacher();

        mockMvc.perform(get("/api/attendance/homeroom-context")
                .header("Authorization", authHeader(token))
                .param("date", "2026-09-24"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.classId").value(testClass.getId().intValue()))
            .andExpect(jsonPath("$.data.academicYearId").value(testAcademicYear.getId().intValue()))
            .andExpect(jsonPath("$.data.shifts").isArray());
    }

    @Test
    void get_homeroom_context_supports_selected_completed_year() throws Exception {
        testAcademicYear.setStatus(
            vn.edu.fpt.myfschool.common.enums.AcademicYearStatus.COMPLETED);
        academicYearRepository.save(testAcademicYear);
        String token = loginAsTeacher();

        mockMvc.perform(get("/api/attendance/homeroom-context")
                .header("Authorization", authHeader(token))
                .param("date", "2026-09-24"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.classId").value(testClass.getId().intValue()))
            .andExpect(jsonPath("$.data.academicYearId")
                .value(testAcademicYear.getId().intValue()));
    }

    @Test
    void get_daily_attendance_student_forbidden() throws Exception {
        String token = loginAsStudent1();

        mockMvc.perform(get("/api/attendance/daily")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("date", "2026-09-24")
                .param("shift", "MORNING"))
            .andExpect(status().isForbidden());
    }

    @Test
    void submit_attendance_teacher_only() throws Exception {
        String token = loginAsTeacher();
        String body = attendanceJson("MORNING",
            entry(testStudent1.getId(), "PRESENT") + "," + entry(testStudent2.getId(), "PRESENT"));

        mockMvc.perform(post("/api/attendance/submit")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            // Backend expands the submission to the full active roster;
            // omitted students are PRESENT by default.
            .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void submit_attendance_parent_forbidden() throws Exception {
        String token = loginAsParent();
        String body = attendanceJson("MORNING", entry(testStudent1.getId(), "PRESENT"));

        mockMvc.perform(post("/api/attendance/submit")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    void submit_absent_only_defaults_remaining_roster_to_present() throws Exception {
        String token = loginAsTeacher();
        String body = attendanceJson(
            "MORNING", entry(testStudent1.getId(), "ABSENT_WITHOUT_LEAVE"));

        mockMvc.perform(post("/api/attendance/submit")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[?(@.studentId == " + testStudent2.getId() + ")].status")
                .value(hasItem("PRESENT")));
    }

    @Test
    void correction_requires_admin_approval_before_attendance_changes() throws Exception {
        String token = loginAsTeacher();
        String sessionBody = "{\"classId\":" + testClass.getId()
            + ",\"teacherId\":" + testTeacher.getId()
            + ",\"date\":\"2026-09-24\",\"shift\":\"MORNING\"}";

        String sessionJson = mockMvc.perform(post("/api/attendance-sessions")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionBody))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Long sessionId = objectMapper.readTree(sessionJson).path("data").path("id").asLong();

        String correctionBody = attendanceJson(
            "MORNING", entry(testStudent1.getId(), "ABSENT_WITHOUT_LEAVE") + ","
                + entry(testStudent2.getId(), "PRESENT") + ","
                + entry(testStudent3.getId(), "PRESENT"));
        String correctionJson = mockMvc.perform(post("/api/attendance/corrections")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(correctionBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andReturn().getResponse().getContentAsString();

        Long correctionId = objectMapper.readTree(correctionJson).path("data").path("id").asLong();
        mockMvc.perform(get("/api/attendance/daily")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("date", "2026-09-24")
                .param("shift", "MORNING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.correctionPending").value(true))
            .andExpect(jsonPath("$.data.students[?(@.studentId == " + testStudent1.getId() + ")].status")
                .value(hasItem("PRESENT")));

        String adminToken = loginAsAdmin();
        mockMvc.perform(put("/api/attendance/admin/corrections/" + correctionId + "/approve")
                .header("Authorization", authHeader(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/attendance/daily")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("date", "2026-09-24")
                .param("shift", "MORNING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.students[?(@.studentId == " + testStudent1.getId() + ")].status")
                .value(hasItem("ABSENT_WITHOUT_LEAVE")));

        assertEquals(AttendanceStatus.ABSENT_WITHOUT_LEAVE,
            attendanceDetailRepository.findBySessionIdAndStudentId(
                sessionId, testStudent1.getId()).orElseThrow().getStatus());
        var projectedSession = attendanceSessionRepository.findById(sessionId).orElseThrow();
        assertEquals(2, projectedSession.getPresent());
        assertEquals(1, projectedSession.getAbsent());
    }

    @Test
    void admin_adjustment_projects_canonical_statuses_and_session_counts() throws Exception {
        String teacherToken = loginAsTeacher();
        String sessionBody = "{\"classId\":" + testClass.getId()
            + ",\"teacherId\":" + testTeacher.getId()
            + ",\"date\":\"2026-09-25\",\"shift\":\"MORNING\"}";
        String sessionJson = mockMvc.perform(post("/api/attendance-sessions")
                .header("Authorization", authHeader(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionBody))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Long sessionId = objectMapper.readTree(sessionJson).path("data").path("id").asLong();

        String adjustmentBody = "{\"academicYearId\":" + testAcademicYear.getId()
            + ",\"classId\":" + testClass.getId()
            + ",\"date\":\"2026-09-25\",\"shift\":\"MORNING\""
            + ",\"presentCount\":2,\"absentWithLeaveCount\":0"
            + ",\"absentWithoutLeaveCount\":1}";
        mockMvc.perform(put("/api/attendance/admin/daily")
                .header("Authorization", authHeader(loginAsAdmin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adjustmentBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.presentCount").value(2))
            .andExpect(jsonPath("$.data.absentWithoutLeaveCount").value(1));

        var session = attendanceSessionRepository.findById(sessionId).orElseThrow();
        assertEquals(3, session.getTotal());
        assertEquals(2, session.getPresent());
        assertEquals(1, session.getAbsent());
        var details = attendanceDetailRepository.findBySessionId(sessionId);
        assertEquals(3, details.size());
        for (var detail : details) {
            var canonical = attendanceRepository.findByStudentIdAndDateAndShift(
                detail.getStudent().getId(), java.time.LocalDate.of(2026, 9, 25),
                Shift.MORNING).orElseThrow();
            assertEquals(canonical.getStatus(), detail.getStatus());
        }
    }

    @Test
    void admin_adjustment_projects_valid_nonzero_excused_absence_with_leave_link()
            throws Exception {
        java.time.LocalDate date = java.time.LocalDate.of(2026, 9, 28);
        LeaveRequest approvedLeave = createApprovedLeave(date, LeaveShift.MORNING);
        String teacherToken = loginAsTeacher();
        String sessionBody = "{\"classId\":" + testClass.getId()
            + ",\"teacherId\":" + testTeacher.getId()
            + ",\"date\":\"2026-09-28\",\"shift\":\"MORNING\"}";
        String sessionJson = mockMvc.perform(post("/api/attendance-sessions")
                .header("Authorization", authHeader(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionBody))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Long sessionId = objectMapper.readTree(sessionJson).path("data").path("id").asLong();

        String adjustmentBody = "{\"academicYearId\":" + testAcademicYear.getId()
            + ",\"classId\":" + testClass.getId()
            + ",\"date\":\"2026-09-28\",\"shift\":\"MORNING\""
            + ",\"presentCount\":2,\"absentWithLeaveCount\":1"
            + ",\"absentWithoutLeaveCount\":0}";
        mockMvc.perform(put("/api/attendance/admin/daily")
                .header("Authorization", authHeader(loginAsAdmin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adjustmentBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.presentCount").value(2))
            .andExpect(jsonPath("$.data.absentWithLeaveCount").value(1));

        var canonical = attendanceRepository.findByStudentIdAndDateAndShift(
            testStudent1.getId(), date, Shift.MORNING).orElseThrow();
        assertEquals(AttendanceStatus.ABSENT_WITH_LEAVE, canonical.getStatus());
        assertEquals(approvedLeave.getId(), canonical.getLeaveRequest().getId());
        assertEquals(AttendanceStatus.ABSENT_WITH_LEAVE,
            attendanceDetailRepository.findBySessionIdAndStudentId(
                sessionId, testStudent1.getId()).orElseThrow().getStatus());
        assertEquals(2,
            attendanceSessionRepository.findById(sessionId).orElseThrow().getPresent());
        assertEquals(1,
            attendanceSessionRepository.findById(sessionId).orElseThrow().getAbsent());
    }

    @Test
    void admin_adjustment_rejects_excused_count_above_eligible_approved_leaves()
            throws Exception {
        java.time.LocalDate date = java.time.LocalDate.of(2026, 9, 29);
        createApprovedLeave(date, LeaveShift.MORNING);
        String adjustmentBody = "{\"academicYearId\":" + testAcademicYear.getId()
            + ",\"classId\":" + testClass.getId()
            + ",\"date\":\"2026-09-29\",\"shift\":\"MORNING\""
            + ",\"presentCount\":1,\"absentWithLeaveCount\":2"
            + ",\"absentWithoutLeaveCount\":0}";

        mockMvc.perform(put("/api/attendance/admin/daily")
                .header("Authorization", authHeader(loginAsAdmin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adjustmentBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));

        assertEquals(0, attendanceRepository.findByClsIdAndDateAndShift(
            testClass.getId(), date, Shift.MORNING).size());
    }

    @Test
    void get_student_attendance_log() throws Exception {
        String token = loginAsStudent1();

        mockMvc.perform(get("/api/attendance/student")
                .header("Authorization", authHeader(token))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.records").isArray())
            .andExpect(jsonPath("$.data.stats").exists());
    }

    @Test
    void get_student_attendance_parent_view() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(get("/api/attendance/student")
                .header("Authorization", authHeader(token))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.stats").exists());
    }

    @Test
    void student_cannot_view_another_students_attendance() throws Exception {
        String token = loginAsStudent1();

        mockMvc.perform(get("/api/attendance/student")
                .header("Authorization", authHeader(token))
                .param("studentId", testStudent2.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    void attendance_stats_contains_all_statuses() throws Exception {
        String token = loginAsTeacher();
        String body = "{\"classId\":" + testClass.getId() + ",\"date\":\"2026-09-26\",\"shift\":\"AFTERNOON\",\"entries\":["
            + entry(testStudent1.getId(), "PRESENT") + ","
            + entry(testStudent2.getId(), "PRESENT") + ","
            + entry(testStudent3.getId(), "ABSENT_WITHOUT_LEAVE") + "]}";

        mockMvc.perform(post("/api/attendance/submit")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());

        String studentToken = loginAsStudent1();
        mockMvc.perform(get("/api/attendance/student")
                .header("Authorization", authHeader(studentToken))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.stats.presentSessions").isNumber());
    }
}
