package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GradeIntegrationTest extends BaseIntegrationTest {

    private String gradeJson(Long studentId, Long subjectId, Long semesterId,
                              double oral, double quiz, double mid, double fin) {
        return "{\"studentId\":" + studentId + ",\"subjectId\":" + subjectId + ",\"semesterId\":" + semesterId
            + ",\"oral\":" + oral + ",\"quiz15m\":" + quiz + ",\"midTerm\":" + mid + ",\"finalScore\":" + fin + "}";
    }

    @Test
    void update_grade_teacher_only() throws Exception {
        String token = loginAsTeacher();

        mockMvc.perform(put("/api/grades")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(gradeJson(testStudent1.getId(), testSubject.getId(), testSemester.getId(), 8.5, 9.0, 7.5, 8.0)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.average").isNumber());
    }

    @Test
    void update_grade_parent_forbidden() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(put("/api/grades")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(gradeJson(testStudent1.getId(), testSubject.getId(), testSemester.getId(), 8.5, 9.0, 7.5, 8.0)))
            .andExpect(status().isForbidden());
    }

    @Test
    void batch_update_grade() throws Exception {
        String token = loginAsTeacher();
        String body = "{\"subjectId\":" + testSubject.getId() + ",\"semesterId\":" + testSemester.getId()
            + ",\"grades\":["
            + "{\"studentId\":" + testStudent1.getId() + ",\"oral\":8.0,\"quiz15m\":7.5,\"midTerm\":9.0,\"finalScore\":8.5},"
            + "{\"studentId\":" + testStudent2.getId() + ",\"oral\":7.0,\"quiz15m\":8.0,\"midTerm\":7.0,\"finalScore\":7.5}"
            + "]}";

        mockMvc.perform(post("/api/grades/batch")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void get_grades_student_semester() throws Exception {
        String token = loginAsStudent1();

        mockMvc.perform(get("/api/grades/semester")
                .header("Authorization", authHeader(token))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void simulate_grade() throws Exception {
        String token = loginAsStudent1();
        String body = "{\"semesterId\":" + testSemester.getId() + ",\"simulations\":["
            + "{\"subjectId\":" + testSubject.getId() + ",\"oral\":9.0,\"quiz15m\":9.0,\"midTerm\":9.0,\"finalScore\":9.0}"
            + "]}";

        mockMvc.perform(post("/api/grades/simulation")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.simulatedGpa").isNumber());
    }
}
