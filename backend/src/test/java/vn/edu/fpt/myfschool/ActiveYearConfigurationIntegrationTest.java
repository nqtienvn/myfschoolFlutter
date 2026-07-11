package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActiveYearConfigurationIntegrationTest extends BaseIntegrationTest {

    @Test
    void activeYear_allowsMasterDataAndHomeroomChanges() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(put("/api/academic-years/" + testAcademicYear.getId() + "/master-data")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectIds\":[],\"shiftIds\":[],\"periodIds\":[]}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/homeroom-assignments")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classId\":%d,\"teacherId\":%d,\"academicYearId\":%d,\"effectiveFrom\":\"2026-08-01\"}"
                    .formatted(testClass.getId(), testTeacher.getId(), testAcademicYear.getId())))
            .andExpect(status().isOk());
    }

    @Test
    void completedYear_rejectsMasterDataAndHomeroomChanges() throws Exception {
        testAcademicYear.setStatus(AcademicYearStatus.COMPLETED);
        academicYearRepository.save(testAcademicYear);
        String token = loginAsAdmin();

        mockMvc.perform(put("/api/academic-years/" + testAcademicYear.getId() + "/master-data")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectIds\":[],\"shiftIds\":[],\"periodIds\":[]}"))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/api/homeroom-assignments")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classId\":%d,\"teacherId\":%d,\"academicYearId\":%d,\"effectiveFrom\":\"2026-08-01\"}"
                    .formatted(testClass.getId(), testTeacher.getId(), testAcademicYear.getId())))
            .andExpect(status().isConflict());
    }
}
