package vn.edu.fpt.myfschool;
import org.junit.jupiter.api.Test; import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class TranscriptIntegrationTest extends BaseIntegrationTest {
 @Test void parent_and_student_can_only_read_linked_published_transcript() throws Exception {String parent=loginAsParent();String student=loginAsStudent1();String other=loginAsStudent2();String url="/api/transcripts/students/"+testStudent1.getId()+"?academicYearId="+testAcademicYear.getId()+"&semesterId="+testSemester.getId();mockMvc.perform(get(url).header("Authorization",authHeader(parent))).andExpect(status().isOk()).andExpect(jsonPath("$.data.studentId").value(testStudent1.getId()));mockMvc.perform(get(url).header("Authorization",authHeader(student))).andExpect(status().isOk());mockMvc.perform(get(url).header("Authorization",authHeader(other))).andExpect(status().isUnauthorized());}
}
