package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GradeBookIntegrationTest extends BaseIntegrationTest {

    @Test
    void get_or_create_creates_gradebook_with_4_default_items() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(get("/api/grade-books")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("subjectId", testSubject.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items.length()").value(4))
            .andExpect(jsonPath("$.data.items[0].name").value("Miệng"));
    }

    @Test
    void update_scores_upserts_and_average_is_weighted() throws Exception {
        String token = loginAsAdmin();
        MvcResult result = createBook(token);
        var items = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("items");
        Long item1Id = items.get(0).get("id").asLong();
        Long item2Id = items.get(1).get("id").asLong();

        mockMvc.perform(put("/api/grade-books/scores")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoresBody(item1Id, 8, 6)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(put("/api/grade-books/scores")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoresBody(item2Id, 5, 7)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].average").value(6.0));

        mockMvc.perform(put("/api/grade-books/scores")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoresBody(item1Id, 9, 6)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].score").value(9));
    }

    @Test
    void finalized_gradebook_rejects_score_updates() throws Exception {
        String token = loginAsAdmin();
        MvcResult result = createBook(token);
        var data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        Long bookId = data.get("id").asLong();
        Long itemId = data.get("items").get(0).get("id").asLong();

        mockMvc.perform(post("/api/grade-books/" + bookId + "/finalize")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk());

        mockMvc.perform(put("/api/grade-books/scores")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoresBody(itemId, 8, 6)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void get_student_scores_returns_all_students_and_all_items() throws Exception {
        String token = loginAsAdmin();
        Long bookId = objectMapper.readTree(createBook(token).getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(get("/api/grade-books/" + bookId + "/students")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(12));
    }

    private MvcResult createBook(String token) throws Exception {
        return mockMvc.perform(get("/api/grade-books")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("subjectId", testSubject.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andReturn();
    }

    private String scoresBody(Long itemId, int firstScore, int secondScore) {
        return "{\"gradeItemId\":" + itemId + ",\"entries\":["
            + "{\"studentId\":" + testStudent1.getId() + ",\"score\":" + firstScore + "},"
            + "{\"studentId\":" + testStudent2.getId() + ",\"score\":" + secondScore + "}"
            + "]}";
    }
}
