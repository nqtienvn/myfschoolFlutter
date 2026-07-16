package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.fpt.myfschool.common.enums.AssessmentType;
import vn.edu.fpt.myfschool.repository.AcademicYearGradeConfigItemRepository;
import vn.edu.fpt.myfschool.repository.StudentScoreAuditRepository;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GradeBookIntegrationTest extends BaseIntegrationTest {
    @Autowired AcademicYearGradeConfigItemRepository configItemRepository;
    @Autowired StudentScoreAuditRepository scoreAuditRepository;

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

    @Test void component_publication_never_leaks_draft_or_edited_scores() throws Exception {
        String teacherToken=loginAsTeacher();var data=book(teacherToken);Long bookId=data.get("id").asLong();
        Long firstItem=data.get("items").get(0).get("id").asLong();Long secondItem=data.get("items").get(1).get("id").asLong();
        mockMvc.perform(put("/api/grade-books/scores").header("Authorization",authHeader(teacherToken)).contentType(MediaType.APPLICATION_JSON).content(scores(firstItem,8,6))).andExpect(status().isOk());
        mockMvc.perform(put("/api/grade-books/scores").header("Authorization",authHeader(teacherToken)).contentType(MediaType.APPLICATION_JSON).content(scores(secondItem,7,5))).andExpect(status().isOk());

        String adminToken=loginAsAdmin();
        mockMvc.perform(post("/api/grade-books/{bookId}/items/{itemId}/publish",bookId,firstItem)
                .header("Authorization",authHeader(adminToken))).andExpect(status().isOk());
        String studentToken=loginAsStudent1();
        mockMvc.perform(get("/api/transcripts/me").header("Authorization",authHeader(studentToken))
                .param("academicYearId",testAcademicYear.getId().toString()).param("semesterId",testSemester.getId().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.subjects[0].scores[0].score").value(8))
            .andExpect(jsonPath("$.data.subjects[0].scores[1].score").doesNotExist());

        mockMvc.perform(put("/api/grade-books/scores").header("Authorization",authHeader(teacherToken)).contentType(MediaType.APPLICATION_JSON).content(scores(firstItem,9,6))).andExpect(status().isOk());
        mockMvc.perform(get("/api/transcripts/me").header("Authorization",authHeader(studentToken))
                .param("academicYearId",testAcademicYear.getId().toString()).param("semesterId",testSemester.getId().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.subjects[0].scores[0].score").doesNotExist());
        mockMvc.perform(post("/api/grade-books/{bookId}/items/{itemId}/publish",bookId,firstItem)
                .header("Authorization",authHeader(adminToken))).andExpect(status().isOk());
        mockMvc.perform(get("/api/transcripts/me").header("Authorization",authHeader(studentToken))
                .param("academicYearId",testAcademicYear.getId().toString()).param("semesterId",testSemester.getId().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.subjects[0].scores[0].score").value(9));
    }

    @Test void published_or_locked_book_requires_all_mandatory_scores() throws Exception {
        String token=loginAsAdmin();var data=book(token);Long id=data.get("id").asLong();
        mockMvc.perform(post("/api/grade-books/"+id+"/status/LOCKED").header("Authorization",authHeader(token))).andExpect(status().isConflict());
    }

    @Test void pass_fail_and_comment_are_validated_audited_and_published_to_transcript() throws Exception {
        var configured=configItemRepository.findByConfigAcademicYearIdOrderByDisplayOrderAsc(testAcademicYear.getId());
        configured.get(0).setAssessmentType(AssessmentType.PASS_FAIL);
        configured.get(1).setAssessmentType(AssessmentType.COMMENT);
        configItemRepository.saveAll(configured);

        String teacherToken=loginAsTeacher();
        var data=book(teacherToken);
        Long passFailOne=data.get("items").get(0).get("id").asLong();
        Long passFailTwo=data.get("items").get(1).get("id").asLong();
        Long commentItem=data.get("items").get(2).get("id").asLong();
        Long scoreItem=data.get("items").get(3).get("id").asLong();

        mockMvc.perform(put("/api/grade-books/scores")
                .header("Authorization",authHeader(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(textValues(passFailOne,"PASS","FAIL","PASS")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].score").doesNotExist())
            .andExpect(jsonPath("$.data[0].comment").value("PASS"))
            .andExpect(jsonPath("$.data[0].isGraded").value(true));
        mockMvc.perform(put("/api/grade-books/scores")
                .header("Authorization",authHeader(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(textValues(passFailTwo,"PASS","FAIL","PASS")))
            .andExpect(status().isOk());
        mockMvc.perform(put("/api/grade-books/scores")
                .header("Authorization",authHeader(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(textValues(passFailOne,"YES","FAIL","PASS")))
            .andExpect(status().isBadRequest());

        String adminToken=loginAsAdmin();
        mockMvc.perform(put("/api/grade-books/scores")
                .header("Authorization",authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(textValues(commentItem,"Tiến bộ tốt","Cần tập trung hơn","Hoàn thành")))
            .andExpect(status().isOk());
        mockMvc.perform(put("/api/grade-books/scores")
                .header("Authorization",authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoresForRoster(scoreItem,9,7,8)))
            .andExpect(status().isOk());

        Long bookId=data.get("id").asLong();
        mockMvc.perform(post("/api/grade-books/"+bookId+"/status/PUBLISHED")
                .header("Authorization",authHeader(adminToken)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/transcripts/me")
                .header("Authorization",authHeader(loginAsStudent1()))
                .param("academicYearId",testAcademicYear.getId().toString())
                .param("semesterId",testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.subjects[0].scores[0].assessmentType").value("PASS_FAIL"))
            .andExpect(jsonPath("$.data.subjects[0].scores[0].comment").value("PASS"))
            .andExpect(jsonPath("$.data.subjects[0].scores[2].assessmentType").value("COMMENT"))
            .andExpect(jsonPath("$.data.subjects[0].scores[2].comment").value("Tiến bộ tốt"))
            .andExpect(jsonPath("$.data.subjects[0].average").value(9.0))
            .andExpect(jsonPath("$.data.subjects[0].complete").value(true));

        org.junit.jupiter.api.Assertions.assertEquals(12,scoreAuditRepository.count());
        var commentAudit=scoreAuditRepository.findAll().stream()
            .filter(audit->"Tiến bộ tốt".equals(audit.getNewComment())).findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE,commentAudit.getNewIsGraded());
    }

    private com.fasterxml.jackson.databind.JsonNode book(String token)throws Exception{MvcResult result=mockMvc.perform(get("/api/grade-books").header("Authorization",authHeader(token)).param("classId",testClass.getId().toString()).param("subjectId",testSubject.getId().toString()).param("semesterId",testSemester.getId().toString())).andExpect(status().isOk()).andReturn();return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");}
    private String scores(Long item,int a,int b){return "{\"gradeItemId\":"+item+",\"reason\":\"Kiểm thử\",\"entries\":[{\"studentId\":"+testStudent1.getId()+",\"score\":"+a+"},{\"studentId\":"+testStudent2.getId()+",\"score\":"+b+"}]}";}
    private String scoresForRoster(Long item,int a,int b,int c){return "{\"gradeItemId\":"+item+",\"reason\":\"Kiểm thử điểm số\",\"entries\":[{\"studentId\":"+testStudent1.getId()+",\"score\":"+a+"},{\"studentId\":"+testStudent2.getId()+",\"score\":"+b+"},{\"studentId\":"+testStudent3.getId()+",\"score\":"+c+"}]}";}
    private String textValues(Long item,String a,String b,String c){return "{\"gradeItemId\":"+item+",\"reason\":\"Kiểm thử đánh giá\",\"entries\":[{\"studentId\":"+testStudent1.getId()+",\"comment\":\""+a+"\"},{\"studentId\":"+testStudent2.getId()+",\"comment\":\""+b+"\"},{\"studentId\":"+testStudent3.getId()+",\"comment\":\""+c+"\"}]}";}
}
