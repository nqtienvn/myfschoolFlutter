package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.entity.AcademicYearSubject;
import vn.edu.fpt.myfschool.entity.Semester;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeachingAssignmentYearScopeIntegrationTest extends BaseIntegrationTest {
    @Autowired vn.edu.fpt.myfschool.repository.AcademicYearSubjectRepository yearSubjectRepository;

    @Test
    void assignOnce_appliesToWholeYearAndReadinessChecksExactSubjects() throws Exception {
        testTeacher.getSubjects().add(testSubject);
        teacherRepository.save(testTeacher);

        AcademicYearSubject yearSubject = new AcademicYearSubject();
        yearSubject.setAcademicYear(testAcademicYear);
        yearSubject.setSubject(testSubject);
        yearSubjectRepository.save(yearSubject);

        Semester semester2 = new Semester();
        semester2.setName("HK II");
        semester2.setAcademicYear(testAcademicYear);
        semester2.setOrder(2);
        semester2.setStartDate(LocalDate.of(2027, 1, 16));
        semester2.setEndDate(LocalDate.of(2027, 5, 31));
        semester2.setIsCurrent(false);
        semesterRepository.save(semester2);

        String token = loginAsAdmin();
        String body = "{\"classId\":%d,\"subjectId\":%d,\"teacherId\":%d}"
            .formatted(testClass.getId(), testSubject.getId(), testTeacher.getId());
        mockMvc.perform(put("/api/teaching-assignments/" + testTeachingAssignment.getId())
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());

        var secondClass = classRepository.findByNameAndAcademicYearId("SE1913", testAcademicYear.getId()).orElseThrow();
        String secondBody = "{\"classId\":%d,\"subjectId\":%d,\"teacherId\":%d}"
            .formatted(secondClass.getId(), testSubject.getId(), testTeacher.getId());
        mockMvc.perform(post("/api/teaching-assignments")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON).content(secondBody))
            .andExpect(status().isOk());

        assertEquals(1, teachingAssignmentRepository.findByClsIdAndStatus(
            testClass.getId(), AssignmentStatus.ACTIVE).size());
        mockMvc.perform(get("/api/teaching-assignments")
                .param("classId", testClass.getId().toString())
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/academic-years/" + testAcademicYear.getId() + "/readiness")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.checks[?(@.code == 'ASSIGNMENTS')].passed").value(true));
    }

}
