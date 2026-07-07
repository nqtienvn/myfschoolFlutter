package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TuitionIntegrationTest extends BaseIntegrationTest {

    private String billJson(Long studentId, Long classId, Long semesterId, String name, long amount) {
        return "{\"studentId\":" + studentId + ",\"classId\":" + classId + ",\"semesterId\":" + semesterId
            + ",\"name\":\"" + name + "\",\"amount\":" + amount + ",\"dueDate\":\"2026-12-31\"}";
    }

    @Test
    void create_tuition_bill_admin_only() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(testStudent1.getId(), testClass.getId(), testSemester.getId(), "Hoc phi HK1", 15000000)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Hoc phi HK1"))
            .andExpect(jsonPath("$.data.status").value("UNPAID"));
    }

    @Test
    void create_tuition_bill_parent_forbidden() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(testStudent1.getId(), testClass.getId(), testSemester.getId(), "Test", 1000000)))
            .andExpect(status().isForbidden());
    }

    @Test
    void teacher_view_class_bills() throws Exception {
        String adminToken = loginAsAdmin();
        String teacherToken = loginAsTeacher();

        mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(testStudent1.getId(), testClass.getId(), testSemester.getId(), "HP List", 10000000)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/tuition/bills/class")
                .header("Authorization", authHeader(teacherToken))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].status").value("UNPAID"));
    }

    @Test
    void parent_view_student_bills() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(get("/api/tuition/bills/student")
                .header("Authorization", authHeader(token))
                .param("studentId", testStudent1.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void simulate_payment() throws Exception {
        String token = loginAsAdmin();

        var result = mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(testStudent1.getId(), testClass.getId(), testSemester.getId(), "HP Pay", 8000000)))
            .andExpect(status().isOk())
            .andReturn();

        Long billId = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(result.getResponse().getContentAsString())
            .get("data").get("id").asLong();

        mockMvc.perform(post("/api/tuition/bills/" + billId + "/simulate-pay")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }
}
