package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.SchoolClass;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StudentEnrollmentIntegrationTest extends BaseIntegrationTest {

    @Test
    void createManually_reusesParentAndEnrollsStudent() throws Exception {
        AcademicYear draft = new AcademicYear();
        draft.setName("2030-2031"); draft.setStartDate(LocalDate.of(2030, 8, 1)); draft.setEndDate(LocalDate.of(2031, 5, 31));
        draft.setStatus(AcademicYearStatus.DRAFT); draft = academicYearRepository.save(draft);
        SchoolClass cls = new SchoolClass(); cls.setName("10A1"); cls.setGradeLevel(10); cls.setAcademicYear(draft); cls.setSchoolName("FPT Schools");
        cls = classRepository.save(cls);

        String token = loginAsAdmin();
        mockMvc.perform(post("/api/admin/student-enrollments")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"academicYearId":%d,"classId":%d,"studentCode":"HS203001","studentName":"Học sinh mới",
                    "dateOfBirth":"2015-01-10","gender":"MALE","parentName":"PH Test","relationship":"FATHER","parentPhone":"0909000002"}
                    """.formatted(draft.getId(), cls.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.studentCode").value("HS203001"))
            .andExpect(jsonPath("$.data.className").value("10A1"))
            .andExpect(jsonPath("$.data.parentReused").value(true));

        var student = studentRepository.findByStudentCode("HS203001").orElseThrow();
        assertTrue(student.getUser().getMustChangePassword());
        assertTrue(enrollmentRepository.findByStudentIdAndAcademicYearIdAndStatus(student.getId(), draft.getId(), vn.edu.fpt.myfschool.common.enums.EnrollmentStatus.ACTIVE).isPresent());
        assertTrue(studentGuardianRepository.existsByStudentIdAndGuardianId(student.getId(), testParent.getId()));

        mockMvc.perform(get("/api/admin/student-enrollments")
                .param("academicYearId", draft.getId().toString())
                .param("classId", cls.getId().toString())
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].studentName").value("Học sinh mới"))
            .andExpect(jsonPath("$.data[0].studentUsername").value("HS203001"))
            .andExpect(jsonPath("$.data[0].guardians[0].parentName").value("PH Test"))
            .andExpect(jsonPath("$.data[0].guardians[0].parentUsername").value("0909000002"))
            .andExpect(jsonPath("$.data[0].guardians[0].relationship").value("FATHER"));
    }

    @Test
    void createManually_activeYear_isRejected() throws Exception {
        mockMvc.perform(post("/api/admin/student-enrollments")
                .header("Authorization", authHeader(loginAsAdmin())).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"academicYearId":%d,"classId":%d,"studentCode":"HS-ACTIVE","studentName":"Học sinh mới",
                    "dateOfBirth":"2015-01-10","gender":"FEMALE","parentName":"Phụ huynh mới","relationship":"MOTHER","parentPhone":"0901234567"}
                    """.formatted(testAcademicYear.getId(), testClass.getId())))
            .andExpect(status().isConflict());
    }
}
