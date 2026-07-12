package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.entity.Attendance;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.repository.AttendanceRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasItem;

class LeaveRequestIntegrationTest extends BaseIntegrationTest {

    @Autowired private AttendanceRepository attendanceRepository;

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
        Attendance attendance = new Attendance();
        attendance.setStudent(testStudent1);
        attendance.setCls(testClass);
        attendance.setTeacher(testTeacher);
        attendance.setDate(java.time.LocalDate.of(2026, 9, 19));
        attendance.setShift(Shift.MORNING);
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendanceRepository.save(attendance);

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

        mockMvc.perform(get("/api/attendance/daily")
                .header("Authorization", authHeader(teacherToken))
                .param("classId", testClass.getId().toString())
                .param("date", "2026-09-19")
                .param("shift", "MORNING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.students[?(@.studentId == " + testStudent1.getId() + ")].status")
                .value(hasItem("PRESENT")));
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
