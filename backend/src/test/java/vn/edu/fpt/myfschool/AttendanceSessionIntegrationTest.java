package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.fpt.myfschool.repository.AttendanceDetailRepository;
import vn.edu.fpt.myfschool.repository.AttendanceSessionRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AttendanceSessionIntegrationTest extends BaseIntegrationTest {

    @Autowired private AttendanceSessionRepository sessionRepository;
    @Autowired private AttendanceDetailRepository detailRepository;

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
}
