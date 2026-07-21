package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.SemesterStatus;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.AcademicYearSubject;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResultFileIsolationIntegrationTest extends BaseIntegrationTest {

    @Test
    void template_containsOnlyAdminGradeItemsFromSelectedYearConfiguration() throws Exception {
        AcademicYearSubject appliedSubject = new AcademicYearSubject();
        appliedSubject.setAcademicYear(testAcademicYear);
        appliedSubject.setSubject(testSubject);
        academicYearSubjectRepository.save(appliedSubject);
        String token = loginAsAdmin();

        MvcResult result = mockMvc.perform(get("/api/result-files/template")
                        .header("Authorization", authHeader(token))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString())
                        .param("classId", testClass.getId().toString())
                        .param("subjectId", testSubject.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();

        byte[] template = result.getResponse().getContentAsByteArray();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(template))) {
            var header = workbook.getSheet("Nhap_diem_Admin").getRow(3);
            assertEquals("GK_1 · Giữa kỳ", header.getCell(2).getStringCellValue());
            assertEquals("CK_1 · Cuối kỳ", header.getCell(3).getStringCellValue());
            assertTrue(workbook.isSheetHidden(workbook.getSheetIndex("_META")));
        }

        testSemester.setStatus(SemesterStatus.COMPLETED);
        semesterRepository.saveAndFlush(testSemester);
        MockMultipartFile lockedImport = new MockMultipartFile("file", "grades.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", template);
        mockMvc.perform(multipart("/api/result-files/import")
                        .file(lockedImport)
                        .header("Authorization", authHeader(token))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString())
                        .param("classId", testClass.getId().toString())
                        .param("subjectId", testSubject.getId().toString()))
                .andExpect(status().isConflict());
    }

    @Test
    void templateAndImport_rejectClassAndSemesterFromAnotherAcademicYear() throws Exception {
        AcademicYear otherYear = new AcademicYear();
        otherYear.setName("2027-2028");
        otherYear.setStartDate(LocalDate.of(2027, 8, 1));
        otherYear.setEndDate(LocalDate.of(2028, 5, 31));
        otherYear.setStatus(AcademicYearStatus.DRAFT);
        otherYear = academicYearRepository.save(otherYear);
        String token = loginAsAdmin();

        mockMvc.perform(get("/api/result-files/template")
                        .header("Authorization", authHeader(token))
                        .param("academicYearId", otherYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString())
                        .param("classId", testClass.getId().toString())
                        .param("subjectId", testSubject.getId().toString()))
                .andExpect(status().isForbidden());

        MockMultipartFile file = new MockMultipartFile("file", "grades.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1, 2, 3});
        mockMvc.perform(multipart("/api/result-files/import")
                        .file(file)
                        .header("Authorization", authHeader(token))
                        .param("academicYearId", otherYear.getId().toString())
                        .param("semesterId", testSemester.getId().toString())
                        .param("classId", testClass.getId().toString())
                        .param("subjectId", testSubject.getId().toString()))
                .andExpect(status().isForbidden());
    }
}
