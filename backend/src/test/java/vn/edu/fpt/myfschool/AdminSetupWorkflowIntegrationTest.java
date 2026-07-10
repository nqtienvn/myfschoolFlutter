package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.repository.PeriodRepository;
import vn.edu.fpt.myfschool.repository.SchoolShiftRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminSetupWorkflowIntegrationTest extends BaseIntegrationTest {
    @Autowired SchoolShiftRepository shiftRepository;
    @Autowired PeriodRepository periodRepository;

    @Test
    void completeEightStepSetup_canActivateDraftYear() throws Exception {
        String token = loginAsAdmin();
        mockMvc.perform(post("/api/master-data/initialize").header("Authorization", authHeader(token)))
            .andExpect(status().isOk());

        String yearResponse = mockMvc.perform(post("/api/academic-years")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDate\":\"2032-08-01\",\"endDate\":\"2033-05-31\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long yearId = objectMapper.readTree(yearResponse).path("data").path("id").asLong();
        var semesters = semesterRepository.findByAcademicYearIdOrderByOrderAsc(yearId);

        long shiftId = shiftRepository.findAllByOrderByOrderAsc().getFirst().getId();
        long periodId = periodRepository.findByShiftIdOrderByOrderAsc(shiftId).getFirst().getId();
        mockMvc.perform(put("/api/academic-years/" + yearId + "/master-data")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectIds\":[%d],\"shiftIds\":[%d],\"periodIds\":[%d]}".formatted(testSubject.getId(), shiftId, periodId)))
            .andExpect(status().isOk());

        String classResponse = mockMvc.perform(post("/api/classes/generate")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"academicYearId\":%d,\"gradeLevel\":10,\"namingPrefix\":\"A\",\"count\":1}".formatted(yearId)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long classId = objectMapper.readTree(classResponse).path("data").get(0).path("id").asLong();

        testTeacher.getSubjects().add(testSubject);
        teacherRepository.save(testTeacher);
        mockMvc.perform(post("/api/homeroom-assignments")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classId\":%d,\"teacherId\":%d,\"academicYearId\":%d,\"effectiveFrom\":\"2032-08-01\"}".formatted(classId, testTeacher.getId(), yearId)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/student-enrollments")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"academicYearId":%d,"classId":%d,"studentCode":"HS203201","studentName":"Học sinh workflow",
                    "dateOfBirth":"2016-01-10","gender":"FEMALE","parentName":"PH Test","relationship":"MOTHER","parentPhone":"0909000002"}
                    """.formatted(yearId, classId)))
            .andExpect(status().isOk());

        for (var semester : semesters) {
            mockMvc.perform(post("/api/teaching-assignments")
                    .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"classId\":%d,\"subjectId\":%d,\"teacherId\":%d,\"semesterId\":%d,\"effectiveFrom\":\"2032-08-01\"}"
                        .formatted(classId, testSubject.getId(), testTeacher.getId(), semester.getId())))
                .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/academic-years/" + yearId + "/readiness").header("Authorization", authHeader(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.ready").value(true));

        testAcademicYear.setStatus(AcademicYearStatus.COMPLETED);
        academicYearRepository.save(testAcademicYear);
        mockMvc.perform(post("/api/academic-years/" + yearId + "/activate").header("Authorization", authHeader(token)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/academic-years").header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.id == %d)].status".formatted(yearId)).value("ACTIVE"));
    }
}
