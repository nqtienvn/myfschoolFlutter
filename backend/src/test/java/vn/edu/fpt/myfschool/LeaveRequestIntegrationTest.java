package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LeaveRequestIntegrationTest extends BaseIntegrationTest {

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
                .content(leaveJson(testStudent1.getId(), "2026-07-01", "2026-07-01", "FULL_DAY", "Con bi om")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.shift").value("FULL_DAY"));
    }

    @Test
    void create_leave_request_student_forbidden() throws Exception {
        String token = loginAsStudent1();

        mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-07-02", "2026-07-02", "MORNING", "Test")))
            .andExpect(status().isForbidden());
    }

    @Test
    void create_leave_request_empty_reason_fails() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-07-04", "2026-07-04", "FULL_DAY", "")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void create_leave_request_date_from_after_date_to_fails() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-07-10", "2026-07-05", "FULL_DAY", "Ngay sai")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void parent_view_my_leave_requests() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-07-11", "2026-07-11", "MORNING", "Don 1")))
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
                .content(leaveJson(testStudent1.getId(), "2026-07-20", "2026-07-22", "FULL_DAY", "Don dau")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/leave-requests")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson(testStudent1.getId(), "2026-07-21", "2026-07-21", "MORNING", "Don trung")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }
}
