package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.fpt.myfschool.common.enums.AssessmentType;
import vn.edu.fpt.myfschool.repository.AcademicYearGradeConfigItemRepository;
import vn.edu.fpt.myfschool.repository.GradeBookRepository;
import vn.edu.fpt.myfschool.repository.StudentScoreRepository;
import vn.edu.fpt.myfschool.repository.StudentScoreAuditRepository;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GradeBookIntegrationTest extends BaseIntegrationTest {
    @Autowired AcademicYearGradeConfigItemRepository configItemRepository;
    @Autowired GradeBookRepository gradeBookRepository;
    @Autowired StudentScoreRepository studentScoreRepository;
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

    @Test void teacher_submit_publishes_new_and_edited_scores_without_creating_notifications() throws Exception {
        String teacherToken=loginAsTeacher();var data=book(teacherToken);
        Long firstItem=data.get("items").get(0).get("id").asLong();
        mockMvc.perform(put("/api/grade-books/scores").header("Authorization",authHeader(teacherToken)).contentType(MediaType.APPLICATION_JSON).content(scores(firstItem,8,6))).andExpect(status().isOk());
        mockMvc.perform(get("/api/grade-books").header("Authorization",authHeader(teacherToken))
                .param("classId",testClass.getId().toString()).param("subjectId",testSubject.getId().toString()).param("semesterId",testSemester.getId().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        String studentToken=loginAsStudent1();
        String parentToken=loginAsParent();
        mockMvc.perform(get("/api/notifications").header("Authorization",authHeader(studentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());
        mockMvc.perform(get("/api/notifications").header("Authorization",authHeader(parentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());

        var legacyScore=studentScoreRepository.findByGradeItemIdAndStudentId(firstItem,testStudent1.getId()).orElseThrow();
        legacyScore.setPublishedAt(null);studentScoreRepository.save(legacyScore);
        var legacyBook=gradeBookRepository.findById(data.get("id").asLong()).orElseThrow();
        legacyBook.setStatus(vn.edu.fpt.myfschool.common.enums.GradeBookStatus.DRAFT);gradeBookRepository.save(legacyBook);

        mockMvc.perform(get("/api/transcripts/me").header("Authorization",authHeader(studentToken))
                .param("academicYearId",testAcademicYear.getId().toString()).param("semesterId",testSemester.getId().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.subjects[0].scores[0].score").value(8))
            .andExpect(jsonPath("$.data.subjects[0].scores[1].score").doesNotExist());
        mockMvc.perform(get("/api/transcripts/students/{studentId}",testStudent1.getId())
                .header("Authorization",authHeader(parentToken))
                .param("academicYearId",testAcademicYear.getId().toString()).param("semesterId",testSemester.getId().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.subjects[0].scores[0].score").value(8));
        mockMvc.perform(put("/api/grade-books/scores").header("Authorization",authHeader(teacherToken)).contentType(MediaType.APPLICATION_JSON).content(scores(firstItem,9,6))).andExpect(status().isOk());
        mockMvc.perform(get("/api/transcripts/me").header("Authorization",authHeader(studentToken))
                .param("academicYearId",testAcademicYear.getId().toString()).param("semesterId",testSemester.getId().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.subjects[0].scores[0].score").value(9));
        mockMvc.perform(get("/api/notifications").header("Authorization",authHeader(parentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test void transcript_keeps_scores_attached_when_configuration_code_changes() throws Exception {
        String teacherToken=loginAsTeacher();var data=book(teacherToken);
        Long firstItem=data.get("items").get(0).get("id").asLong();
        mockMvc.perform(put("/api/grade-books/scores")
                .header("Authorization",authHeader(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(scores(firstItem,8,6)))
            .andExpect(status().isOk());

        var configured=configItemRepository.findByConfigAcademicYearIdOrderByDisplayOrderAsc(testAcademicYear.getId());
        configured.getFirst().setCode("TX_NEW");
        configured.getFirst().setDisplayName("Thường xuyên mới");
        configItemRepository.save(configured.getFirst());

        mockMvc.perform(get("/api/transcripts/me")
                .header("Authorization",authHeader(loginAsStudent1()))
                .param("academicYearId",testAcademicYear.getId().toString())
                .param("semesterId",testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.subjects[0].scores[0].code").value("TX_NEW_1"))
            .andExpect(jsonPath("$.data.subjects[0].scores[0].name").value("Thường xuyên mới 1"))
            .andExpect(jsonPath("$.data.subjects[0].scores[0].score").value(8))
            .andExpect(jsonPath("$.data.subjects[0].scores[0].isGraded").value(true));
    }

    @Test void transcript_uses_year_configuration_for_full_empty_table_and_rejects_cross_year_scope() throws Exception {
        var physics=new vn.edu.fpt.myfschool.entity.Subject();physics.setName("Vật lý");physics.setCode("VATLY12");physics=subjectRepository.save(physics);
        var appliedPhysics=new vn.edu.fpt.myfschool.entity.AcademicYearSubject();appliedPhysics.setAcademicYear(testAcademicYear);appliedPhysics.setSubject(physics);academicYearSubjectRepository.save(appliedPhysics);

        String studentToken=loginAsStudent1();
        mockMvc.perform(get("/api/transcripts/me").header("Authorization",authHeader(studentToken))
                .param("academicYearId",testAcademicYear.getId().toString()).param("semesterId",testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.subjects.length()").value(2))
            .andExpect(jsonPath("$.data.subjects[0].subjectName").value("Toán"))
            .andExpect(jsonPath("$.data.subjects[0].scores.length()").value(4))
            .andExpect(jsonPath("$.data.subjects[0].scores[0].code").value("TX_1"))
            .andExpect(jsonPath("$.data.subjects[0].scores[1].code").value("TX_2"))
            .andExpect(jsonPath("$.data.subjects[0].scores[2].code").value("GK_1"))
            .andExpect(jsonPath("$.data.subjects[0].scores[0].score").doesNotExist())
            .andExpect(jsonPath("$.data.subjects[0].scores[0].isGraded").value(false))
            .andExpect(jsonPath("$.data.subjects[1].subjectName").value("Vật lý"))
            .andExpect(jsonPath("$.data.subjects[1].scores.length()").value(4));

        var otherYear=new vn.edu.fpt.myfschool.entity.AcademicYear();otherYear.setName("2027-2028");
        otherYear.setStartDate(java.time.LocalDate.of(2027,8,1));otherYear.setEndDate(java.time.LocalDate.of(2028,5,31));
        otherYear.setStatus(vn.edu.fpt.myfschool.common.enums.AcademicYearStatus.DRAFT);otherYear=academicYearRepository.save(otherYear);
        var otherSemester=new vn.edu.fpt.myfschool.entity.Semester();otherSemester.setName("HK I");otherSemester.setOrder(1);
        otherSemester.setStartDate(java.time.LocalDate.of(2027,9,1));otherSemester.setEndDate(java.time.LocalDate.of(2028,1,15));
        otherSemester.setIsCurrent(false);otherSemester.setAcademicYear(otherYear);otherSemester=semesterRepository.save(otherSemester);

        mockMvc.perform(get("/api/transcripts/me").header("Authorization",authHeader(studentToken))
                .param("academicYearId",testAcademicYear.getId().toString()).param("semesterId",otherSemester.getId().toString()))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/grade-books").header("Authorization",authHeader(loginAsAdmin()))
                .param("classId",testClass.getId().toString()).param("subjectId",testSubject.getId().toString()).param("semesterId",otherSemester.getId().toString()))
            .andExpect(status().isBadRequest());
    }

    @Test void admin_cannot_publish_and_cannot_lock_incomplete_book() throws Exception {
        String token=loginAsAdmin();var data=book(token);Long id=data.get("id").asLong();
        mockMvc.perform(post("/api/grade-books/"+id+"/status/PUBLISHED").header("Authorization",authHeader(token))).andExpect(status().isBadRequest());
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
