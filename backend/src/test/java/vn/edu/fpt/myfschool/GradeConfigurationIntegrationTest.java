package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GradeConfigurationIntegrationTest extends BaseIntegrationTest {
    @Test void applyingTemplateMakesGradeReadinessPassForDraftYear() throws Exception {
        AcademicYear year = new AcademicYear(); year.setName("2050-2051");
        year.setStartDate(LocalDate.of(2050,8,1)); year.setEndDate(LocalDate.of(2051,5,31));
        year.setStatus(AcademicYearStatus.DRAFT); year = academicYearRepository.save(year);
        String token = loginAsAdmin();
        String templateResponse = mockMvc.perform(post("/api/grade-configurations/templates")
                .header("Authorization",authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Mẫu kiểm thử\",\"items\":[{\"code\":\"TX\",\"displayName\":\"Thường xuyên\",\"weight\":1,\"quantity\":2,\"entryRole\":\"SUBJECT_TEACHER\",\"assessmentType\":\"SCORE\",\"requiredEntry\":true,\"displayOrder\":0}]}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long templateId = objectMapper.readTree(templateResponse).path("data").path("id").asLong();
        mockMvc.perform(post("/api/grade-configurations/academic-years/"+year.getId()+"/templates/"+templateId)
                .header("Authorization",authHeader(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.academicYearId").value(year.getId()));
        mockMvc.perform(get("/api/academic-years/"+year.getId()+"/readiness").header("Authorization",authHeader(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.checks[1].code").value("GRADE_CONFIG"))
            .andExpect(jsonPath("$.data.checks[1].passed").value(true));
    }
}
