package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.fpt.myfschool.repository.AttendanceDetailRepository;
import vn.edu.fpt.myfschool.repository.AttendanceSessionRepository;
import vn.edu.fpt.myfschool.repository.AttendanceRepository;
import vn.edu.fpt.myfschool.repository.ScheduleRepository;
import vn.edu.fpt.myfschool.repository.TimetableRepository;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.entity.Attendance;
import vn.edu.fpt.myfschool.entity.AttendanceSession;
import vn.edu.fpt.myfschool.entity.Schedule;
import vn.edu.fpt.myfschool.entity.Timetable;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.TimetableStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AttendanceSessionIntegrationTest extends BaseIntegrationTest {

    @Autowired private AttendanceSessionRepository sessionRepository;
    @Autowired private AttendanceDetailRepository detailRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private TimetableRepository timetableRepository;
    @Autowired private ScheduleRepository scheduleRepository;

    @BeforeEach
    void configureAttendanceScope() {
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

    @Test
    void create_session_auto_populates_3_student_details() throws Exception {
        String token = loginAsTeacher();
        String body = "{\"classId\":" + testClass.getId()
            + ",\"teacherId\":" + testTeacher.getId()
            + ",\"date\":\"2026-09-01\",\"shift\":\"MORNING\"}";

        mockMvc.perform(post("/api/attendance-sessions")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.total").value(3))
            .andExpect(jsonPath("$.data.present").value(3))
            .andExpect(jsonPath("$.data.absent").value(0))
            .andExpect(jsonPath("$.data.isClosed").value(false))
            .andExpect(jsonPath("$.data.details.length()").value(3));
    }

    @Test
    void create_session_preserves_existing_canonical_absence() throws Exception {
        java.time.LocalDate date = java.time.LocalDate.of(2026, 9, 8);
        Attendance existing = new Attendance();
        existing.setStudent(testStudent1);
        existing.setCls(testClass);
        existing.setTeacher(testTeacher);
        existing.setDate(date);
        existing.setShift(Shift.MORNING);
        existing.setStatus(AttendanceStatus.ABSENT_WITHOUT_LEAVE);
        existing = attendanceRepository.saveAndFlush(existing);

        String body = "{\"classId\":" + testClass.getId()
            + ",\"teacherId\":" + testTeacher.getId()
            + ",\"date\":\"2026-09-08\",\"shift\":\"MORNING\"}";

        mockMvc.perform(post("/api/attendance-sessions")
                .header("Authorization", authHeader(loginAsTeacher()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(3))
            .andExpect(jsonPath("$.data.present").value(2))
            .andExpect(jsonPath("$.data.absent").value(1))
            .andExpect(jsonPath("$.data.details[?(@.studentId == "
                + testStudent1.getId() + ")].status")
                .value(org.hamcrest.Matchers.hasItem("ABSENT_WITHOUT_LEAVE")));

        assertEquals(AttendanceStatus.ABSENT_WITHOUT_LEAVE,
            attendanceRepository.findById(existing.getId()).orElseThrow().getStatus());
    }

    @Test
    void update_details_recalculates_counts() throws Exception {
        String token = loginAsTeacher();
        String createBody = "{\"classId\":" + testClass.getId()
            + ",\"teacherId\":" + testTeacher.getId()
            + ",\"date\":\"2026-09-02\",\"shift\":\"MORNING\"}";

        MvcResult result = mockMvc.perform(post("/api/attendance-sessions")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
            .andExpect(status().isOk())
            .andReturn();

        Long sessionId = objectMapper.readTree(result.getResponse().getContentAsString())
            .get("data").get("id").asLong();

        String updateBody = "{\"sessionId\":" + sessionId
            + ",\"entries\":["
            + "{\"studentId\":" + testStudent1.getId() + ",\"status\":\"ABSENT_WITH_LEAVE\",\"note\":\"\"},"
            + "{\"studentId\":" + testStudent2.getId() + ",\"status\":\"ABSENT_WITHOUT_LEAVE\",\"note\":\"\"}"
            + "]}";

        mockMvc.perform(put("/api/attendance-sessions/" + sessionId + "/details")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify counts updated
        mockMvc.perform(get("/api/attendance-sessions")
                .header("Authorization", authHeader(token))
                .param("classId", String.valueOf(testClass.getId()))
                .param("date", "2026-09-02")
                .param("shift", "MORNING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].present").value(1))
            .andExpect(jsonPath("$.data[0].absent").value(2));

        // The canonical writer normalizes an unapproved "with leave" status.
        assertEquals(AttendanceStatus.ABSENT_WITHOUT_LEAVE,
            attendanceRepository.findByStudentIdAndDateAndShift(
                testStudent1.getId(), java.time.LocalDate.of(2026, 9, 2), Shift.MORNING)
                .orElseThrow().getStatus());
        assertEquals(AttendanceStatus.ABSENT_WITHOUT_LEAVE,
            attendanceRepository.findByStudentIdAndDateAndShift(
                testStudent2.getId(), java.time.LocalDate.of(2026, 9, 2), Shift.MORNING)
                .orElseThrow().getStatus());
    }

    @Test
    void create_duplicate_session_throws_conflict() throws Exception {
        String token = loginAsTeacher();
        String body = "{\"classId\":" + testClass.getId()
            + ",\"teacherId\":" + testTeacher.getId()
            + ",\"date\":\"2026-09-03\",\"shift\":\"MORNING\"}";

        // First create
        mockMvc.perform(post("/api/attendance-sessions")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());

        // Duplicate → conflict
        mockMvc.perform(post("/api/attendance-sessions")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void database_rejects_duplicate_class_date_shift_without_schedule() {
        java.time.LocalDate date = java.time.LocalDate.of(2026, 9, 9);
        sessionRepository.saveAndFlush(attendanceSession(date));

        assertThrows(DataIntegrityViolationException.class,
            () -> sessionRepository.saveAndFlush(attendanceSession(date)));
    }

    @Test
    void admin_can_list_attendance_sessions() throws Exception {
        String body = "{\"classId\":" + testClass.getId()
            + ",\"teacherId\":" + testTeacher.getId()
            + ",\"date\":\"2026-09-10\",\"shift\":\"MORNING\"}";
        mockMvc.perform(post("/api/attendance-sessions")
                .header("Authorization", authHeader(loginAsTeacher()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/attendance-sessions")
                .header("Authorization", authHeader(loginAsAdmin()))
                .param("classId", testClass.getId().toString())
                .param("date", "2026-09-10")
                .param("shift", "MORNING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void teacher_role_without_profile_cannot_list_attendance_sessions() throws Exception {
        User user = new User();
        user.setPhone("0909000098");
        user.setPassword(passwordEncoder.encode("test1234"));
        user.setName("GV thiếu hồ sơ");
        user.setRole(UserRole.TEACHER);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/attendance-sessions")
                .header("Authorization", authHeader(login("0909000098", "test1234")))
                .param("classId", testClass.getId().toString())
                .param("date", "2026-09-10")
                .param("shift", "MORNING"))
            .andExpect(status().isNotFound());
    }

    @Test
    void close_session_then_update_fails() throws Exception {
        String token = loginAsTeacher();
        String createBody = "{\"classId\":" + testClass.getId()
            + ",\"teacherId\":" + testTeacher.getId()
            + ",\"date\":\"2026-09-04\",\"shift\":\"MORNING\"}";

        MvcResult result = mockMvc.perform(post("/api/attendance-sessions")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
            .andExpect(status().isOk())
            .andReturn();

        Long sessionId = objectMapper.readTree(result.getResponse().getContentAsString())
            .get("data").get("id").asLong();

        // Close
        mockMvc.perform(post("/api/attendance-sessions/" + sessionId + "/close")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isClosed").value(true));

        // Update after close → should fail
        String updateBody = "{\"sessionId\":" + sessionId
            + ",\"entries\":[{\"studentId\":" + testStudent1.getId() + ",\"status\":\"PRESENT\",\"note\":\"\"}]}";

        mockMvc.perform(put("/api/attendance-sessions/" + sessionId + "/details")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    void student_forbidden_from_creating_session() throws Exception {
        String token = loginAsStudent1();
        String body = "{\"classId\":" + testClass.getId()
            + ",\"teacherId\":" + testTeacher.getId()
            + ",\"date\":\"2026-09-05\",\"shift\":\"MORNING\"}";

        mockMvc.perform(post("/api/attendance-sessions")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    void teacher_id_must_match_authenticated_teacher() throws Exception {
        String token = loginAsTeacher();
        String body = "{\"classId\":" + testClass.getId()
            + ",\"teacherId\":" + (testTeacher.getId() + 1000)
            + ",\"date\":\"2026-09-06\",\"shift\":\"MORNING\"}";

        mockMvc.perform(post("/api/attendance-sessions")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void path_session_id_must_match_request_body() throws Exception {
        String token = loginAsTeacher();
        String createBody = "{\"classId\":" + testClass.getId()
            + ",\"teacherId\":" + testTeacher.getId()
            + ",\"date\":\"2026-09-07\",\"shift\":\"MORNING\"}";
        MvcResult result = mockMvc.perform(post("/api/attendance-sessions")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
            .andExpect(status().isOk())
            .andReturn();
        Long sessionId = objectMapper.readTree(result.getResponse().getContentAsString())
            .get("data").get("id").asLong();

        String updateBody = "{\"sessionId\":" + (sessionId + 1)
            + ",\"entries\":[{\"studentId\":" + testStudent1.getId()
            + ",\"status\":\"PRESENT\",\"note\":\"\"}]}";
        mockMvc.perform(put("/api/attendance-sessions/" + sessionId + "/details")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
            .andExpect(status().isBadRequest());
    }

    private AttendanceSession attendanceSession(java.time.LocalDate date) {
        AttendanceSession session = new AttendanceSession();
        session.setCls(testClass);
        session.setTeacher(testTeacher);
        session.setDate(date);
        session.setShift(Shift.MORNING);
        return session;
    }
}
