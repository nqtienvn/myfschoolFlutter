package vn.edu.fpt.myfschool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.SemesterStatus;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.AcademicYearSubject;
import vn.edu.fpt.myfschool.entity.GradeBook;
import vn.edu.fpt.myfschool.entity.Subject;
import vn.edu.fpt.myfschool.repository.GradeBookRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminGradeImportIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private GradeBookRepository gradeBookRepository;

    @Test
    void downloadsAndImportsWhenAnExistingGradeBookHasNoMatchingGradeItem() throws Exception {
        activateSemesterAndSubjects();
        Subject vietnamese = subjectRepository.findByCode("VAN12").orElseThrow();
        GradeBook incompleteBook = new GradeBook();
        incompleteBook.setCls(testClass);
        incompleteBook.setSubject(vietnamese);
        incompleteBook.setSemester(testSemester);
        gradeBookRepository.saveAndFlush(incompleteBook);

        MvcResult template = mockMvc.perform(get("/api/admin-grade-imports/template/CK_1")
                        .header("Authorization", authHeader(loginAsAdmin())))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        result.getResponse().getContentType()))
                .andReturn();

        MockMultipartFile file = new MockMultipartFile("file", "ck.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                withScores(template.getResponse().getContentAsByteArray()));
        mockMvc.perform(multipart("/api/admin-grade-imports/import").file(file)
                        .header("Authorization", authHeader(loginAsAdmin())))
                .andExpect(status().isOk());
    }

    @Test
    void importsOneAdminGradeItemForAllSubjectsAndKeepsExcelOrder() throws Exception {
        activateSemesterAndSubjects();
        String token = loginAsAdmin();

        MvcResult templateResult = mockMvc.perform(get("/api/admin-grade-imports/template/GK_1")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andReturn();

        byte[] template = withScores(templateResult.getResponse().getContentAsByteArray());
        MockMultipartFile file = new MockMultipartFile("file", "gk.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", template);
        MvcResult importResult = mockMvc.perform(multipart("/api/admin-grade-imports/import")
                        .file(file).header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRows").value(3))
                .andExpect(jsonPath("$.data.updatedScores").value(6))
                .andReturn();
        long batchId = objectMapper.readTree(importResult.getResponse().getContentAsString()).get("data").get("batchId").asLong();

        mockMvc.perform(get("/api/admin-grade-imports/batches/{batchId}", batchId)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[0].studentCode").value("12A-01"))
                .andExpect(jsonPath("$.data.rows[0].sourceOrder").value(1))
                .andExpect(jsonPath("$.data.rows[0].cells[0].score").value(8.0));
    }

    @Test
    void rejectsTemplateWhoseAcademicYearMetadataWasChanged() throws Exception {
        activateSemesterAndSubjects();
        String token = loginAsAdmin();
        MvcResult templateResult = mockMvc.perform(get("/api/admin-grade-imports/template/GK_1")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk()).andReturn();
        AcademicYear otherYear = new AcademicYear();
        otherYear.setName("2027-2028");
        otherYear.setStartDate(LocalDate.of(2027, 8, 1));
        otherYear.setEndDate(LocalDate.of(2028, 5, 31));
        otherYear.setStatus(AcademicYearStatus.DRAFT);
        otherYear = academicYearRepository.save(otherYear);

        byte[] tampered = changeAcademicYear(templateResult.getResponse().getContentAsByteArray(), otherYear.getId());
        MockMultipartFile file = new MockMultipartFile("file", "tampered.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", tampered);
        mockMvc.perform(multipart("/api/admin-grade-imports/import").file(file)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isBadRequest());
    }

    private void activateSemesterAndSubjects() {
        testSemester.setStatus(SemesterStatus.ACTIVE);
        semesterRepository.saveAndFlush(testSemester);
        Subject secondSubject = subjectRepository.findByCode("VAN12").orElseThrow();
        for (Subject subject : new Subject[] {testSubject, secondSubject}) {
            AcademicYearSubject applied = new AcademicYearSubject();
            applied.setAcademicYear(testAcademicYear);
            applied.setSubject(subject);
            academicYearSubjectRepository.save(applied);
        }
    }

    private byte[] withScores(byte[] source) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(source));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.getSheet("Nhap_diem_toan_truong");
            assertEquals("Mã học sinh", sheet.getRow(3).getCell(0).getStringCellValue());
            assertTrue(workbook.isSheetHidden(workbook.getSheetIndex("_META")));
            for (int row = 4; row <= 6; row++) {
                sheet.getRow(row).getCell(3).setCellValue(4 + row);
                sheet.getRow(row).getCell(4).setCellValue(2 + row);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private byte[] changeAcademicYear(byte[] source, Long academicYearId) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(source));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var meta = workbook.getSheet("_META");
            for (var row : meta) {
                if ("academicYearId".equals(row.getCell(0).getStringCellValue())) {
                    row.getCell(1).setCellValue(String.valueOf(academicYearId));
                }
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
