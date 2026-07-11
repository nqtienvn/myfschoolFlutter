package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ScheduleIntegrationTest extends BaseIntegrationTest {

    private String schedJson(long timetableId, int day, int period, String room, String shift) {
        return "{\"timetableId\":" + timetableId + ",\"assignmentId\":" + testTeachingAssignment.getId()
            + ",\"dayOfWeek\":" + day + ",\"period\":" + period
            + ",\"room\":\"" + room + "\",\"shift\":\"" + shift + "\"}";
    }

    private long createDraft(String token, String effectiveFrom, Long copyFrom) throws Exception {
        String body = "{\"classId\":%d,\"semesterId\":%d,\"effectiveFrom\":\"%s\"%s}"
            .formatted(testClass.getId(), testSemester.getId(), effectiveFrom,
                copyFrom == null ? "" : ",\"copyFromTimetableId\":" + copyFrom);
        var result = mockMvc.perform(post("/api/timetables")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private void publish(String token, long timetableId, String effectiveFrom) throws Exception {
        mockMvc.perform(post("/api/timetables/" + timetableId + "/publish")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"effectiveFrom\":\"" + effectiveFrom + "\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void get_class_schedule() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(get("/api/schedules/class")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.days").isArray())
            .andExpect(jsonPath("$.data.days.length()").value(7));
    }

    @Test
    void create_schedule_admin_only() throws Exception {
        String token = loginAsAdmin();
        long timetableId = createDraft(token, "2026-09-01", null);

        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(timetableId, 2, 1, "P.101", "MORNING")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.dayOfWeek").value(2))
            .andExpect(jsonPath("$.data.period").value(1));
    }

    @Test
    void create_schedule_duplicate_class_period_fails() throws Exception {
        String token = loginAsAdmin();
        long timetableId = createDraft(token, "2026-09-01", null);

        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(timetableId, 3, 2, "P.102", "MORNING")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(timetableId, 3, 2, "P.103", "MORNING")))
            .andExpect(status().isConflict());
    }

    @Test
    void delete_schedule() throws Exception {
        String token = loginAsAdmin();
        long timetableId = createDraft(token, "2026-09-01", null);

        var createResult = mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(timetableId, 5, 4, "P.106", "AFTERNOON")))
            .andExpect(status().isOk())
            .andReturn();

        Long scheduleId = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(createResult.getResponse().getContentAsString())
            .get("data").get("id").asLong();

        mockMvc.perform(delete("/api/schedules/" + scheduleId).header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void get_available_periods() throws Exception {
        String token = loginAsAdmin();
        long timetableId = createDraft(token, "2026-09-01", null);

        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(timetableId, 6, 1, "P.107", "MORNING")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/schedules/available-periods")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString())
                .param("dayOfWeek", "6")
                .param("shift", "MORNING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(4));
    }

    @Test
    void student_forbidden_to_create_schedule() throws Exception {
        String adminToken = loginAsAdmin();
        long timetableId = createDraft(adminToken, "2026-09-01", null);
        String token = loginAsStudent1();

        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(timetableId, 2, 1, "P.101", "MORNING")))
            .andExpect(status().isForbidden());
    }

    @Test
    void publish_new_version_keeps_old_schedule_by_effective_date() throws Exception {
        String token = loginAsAdmin();
        long version1 = createDraft(token, "2026-09-01", null);
        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(version1, 2, 1, "P.101", "MORNING")))
            .andExpect(status().isOk());
        publish(token, version1, "2026-09-01");

        long version2 = createDraft(token, "2026-11-01", version1);
        publish(token, version2, "2026-11-01");

        mockMvc.perform(get("/api/schedules/class")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString())
                .param("date", "2026-10-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.timetableVersion").value(1));

        mockMvc.perform(get("/api/schedules/class")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString())
                .param("date", "2026-11-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.timetableVersion").value(2));
    }
}
