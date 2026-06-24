package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ScheduleIntegrationTest extends BaseIntegrationTest {

    private String schedJson(Long classId, Long subjectId, Long teacherId, Long semesterId,
                              int day, int period, String room, String shift) {
        return "{\"classId\":" + classId + ",\"subjectId\":" + subjectId + ",\"teacherId\":" + teacherId
            + ",\"semesterId\":" + semesterId + ",\"dayOfWeek\":" + day + ",\"period\":" + period
            + ",\"room\":\"" + room + "\",\"shift\":\"" + shift + "\"}";
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
    void create_schedule_teacher_only() throws Exception {
        String token = loginAsTeacher();

        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(testClass.getId(), testSubject.getId(), testTeacher.getId(),
                    testSemester.getId(), 2, 1, "P.101", "MORNING")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.dayOfWeek").value(2))
            .andExpect(jsonPath("$.data.period").value(1));
    }

    @Test
    void create_schedule_duplicate_class_period_fails() throws Exception {
        String token = loginAsTeacher();

        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(testClass.getId(), testSubject.getId(), testTeacher.getId(),
                    testSemester.getId(), 3, 2, "P.102", "MORNING")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(testClass.getId(), testSubject.getId(), testTeacher.getId(),
                    testSemester.getId(), 3, 2, "P.103", "MORNING")))
            .andExpect(status().isConflict());
    }

    @Test
    void delete_schedule() throws Exception {
        String token = loginAsTeacher();

        var createResult = mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(testClass.getId(), testSubject.getId(), testTeacher.getId(),
                    testSemester.getId(), 5, 4, "P.106", "AFTERNOON")))
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
        String token = loginAsTeacher();

        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(testClass.getId(), testSubject.getId(), testTeacher.getId(),
                    testSemester.getId(), 6, 1, "P.107", "MORNING")))
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
        String token = loginAsStudent1();

        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(testClass.getId(), testSubject.getId(), testTeacher.getId(),
                    testSemester.getId(), 2, 1, "P.101", "MORNING")))
            .andExpect(status().isForbidden());
    }
}
