package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GradeBookIntegrationTest extends BaseIntegrationTest {
    @BeforeEach void applySubjectToYear() {
        var applied = new vn.edu.fpt.myfschool.entity.AcademicYearSubject();
        applied.setAcademicYear(testAcademicYear); applied.setSubject(testSubject);
        academicYearSubjectRepository.save(applied);
    }
    @Test void gradebook_uses_year_configuration_and_is_year_scoped() throws Exception {
        String token=loginAsAdmin();
        mockMvc.perform(get("/api/grade-books").header("Authorization",authHeader(token))
                .param("classId",testClass.getId().toString()).param("subjectId",testSubject.getId().toString()).param("semesterId",testSemester.getId().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(4))
            .andExpect(jsonPath("$.data.items[0].code").value("TX_1"))
            .andExpect(jsonPath("$.data.items[0].entryRole").value("SUBJECT_TEACHER"))
            .andExpect(jsonPath("$.data.items[2].entryRole").value("ADMIN"));
    }

    @Test void admin_can_enter_admin_items_but_not_teacher_items() throws Exception {
        String token=loginAsAdmin();var data=book(token);Long teacherItem=data.get("items").get(0).get("id").asLong();Long adminItem=data.get("items").get(2).get("id").asLong();
        mockMvc.perform(put("/api/grade-books/scores").header("Authorization",authHeader(token)).contentType(MediaType.APPLICATION_JSON).content(scores(adminItem,8,6))).andExpect(status().isOk());
        mockMvc.perform(put("/api/grade-books/scores").header("Authorization",authHeader(token)).contentType(MediaType.APPLICATION_JSON).content(scores(teacherItem,8,6))).andExpect(status().isUnauthorized());
    }

    @Test void assigned_teacher_can_enter_only_teacher_items() throws Exception {
        String token=loginAsTeacher();var data=book(token);Long teacherItem=data.get("items").get(0).get("id").asLong();Long adminItem=data.get("items").get(2).get("id").asLong();
        mockMvc.perform(put("/api/grade-books/scores").header("Authorization",authHeader(token)).contentType(MediaType.APPLICATION_JSON).content(scores(teacherItem,8,6))).andExpect(status().isOk());
        mockMvc.perform(put("/api/grade-books/scores").header("Authorization",authHeader(token)).contentType(MediaType.APPLICATION_JSON).content(scores(adminItem,8,6))).andExpect(status().isUnauthorized());
    }

    @Test void published_or_locked_book_requires_all_mandatory_scores() throws Exception {
        String token=loginAsAdmin();var data=book(token);Long id=data.get("id").asLong();
        mockMvc.perform(post("/api/grade-books/"+id+"/status/LOCKED").header("Authorization",authHeader(token))).andExpect(status().isConflict());
    }

    private com.fasterxml.jackson.databind.JsonNode book(String token)throws Exception{MvcResult result=mockMvc.perform(get("/api/grade-books").header("Authorization",authHeader(token)).param("classId",testClass.getId().toString()).param("subjectId",testSubject.getId().toString()).param("semesterId",testSemester.getId().toString())).andExpect(status().isOk()).andReturn();return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");}
    private String scores(Long item,int a,int b){return "{\"gradeItemId\":"+item+",\"reason\":\"Kiểm thử\",\"entries\":[{\"studentId\":"+testStudent1.getId()+",\"score\":"+a+"},{\"studentId\":"+testStudent2.getId()+",\"score\":"+b+"}]}";}
}
