package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AcademicYearIntegrationTest extends BaseIntegrationTest {

    @Test
    void createAcademicYear_adminRole_returnsCreatedYear() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/academic-years")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"2027-2028\",\"startDate\":\"2027-08-01\",\"endDate\":\"2028-05-31\",\"status\":\"DRAFT\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("2027-2028"))
            .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void activateAcademicYear_whenAnotherYearActive_returnsConflict() throws Exception {
        String token = loginAsAdmin();

        String body = "{\"name\":\"2028-2029\",\"startDate\":\"2028-08-01\",\"endDate\":\"2029-05-31\",\"status\":\"DRAFT\"}";
        String response = mockMvc.perform(post("/api/academic-years")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(put("/api/academic-years/" + id + "/status")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false));
    }
}
