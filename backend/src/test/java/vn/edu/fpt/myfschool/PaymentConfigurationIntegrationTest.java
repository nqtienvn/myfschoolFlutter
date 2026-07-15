package vn.edu.fpt.myfschool;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.Semester;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentConfigurationIntegrationTest extends BaseIntegrationTest {

    @Test
    void admin_configurations_are_isolated_between_academic_years() throws Exception {
        AcademicYear otherYear = createOtherAcademicYear();
        Semester otherSemester = createSemester(otherYear);
        String adminToken = loginAsAdmin();

        putConfiguration(
            adminToken,
            testAcademicYear.getId(),
            "TPB",
            "TPBank",
            "1234567890");
        putConfiguration(
            adminToken,
            otherYear.getId(),
            "VCB",
            "Vietcombank",
            "9988776655");

        mockMvc.perform(get("/api/payment-configurations/academic-years/"
                + testAcademicYear.getId())
                .header("Authorization", authHeader(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.academicYearId")
                .value(testAcademicYear.getId().intValue()))
            .andExpect(jsonPath("$.data.bankCode").value("TPB"))
            .andExpect(jsonPath("$.data.accountNumber").value("1234567890"));

        mockMvc.perform(get("/api/payment-configurations/academic-years/"
                + otherYear.getId())
                .header("Authorization", authHeader(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.academicYearId")
                .value(otherYear.getId().intValue()))
            .andExpect(jsonPath("$.data.bankCode").value("VCB"))
            .andExpect(jsonPath("$.data.accountNumber").value("9988776655"));

        mockMvc.perform(get("/api/payment-configurations/semesters/"
                + otherSemester.getId())
                .header("Authorization", authHeader(loginAsStudent1())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.academicYearId")
                .value(otherYear.getId().intValue()))
            .andExpect(jsonPath("$.data.bankName").value("Vietcombank"))
            .andExpect(jsonPath("$.data.method").value("BANK_TRANSFER"))
            .andExpect(jsonPath("$.data.displayMode").value("MANUAL"))
            .andExpect(jsonPath("$.data.qrAvailable").value(false));
    }

    @Test
    void mobile_get_returns_null_until_admin_configures_the_selected_year() throws Exception {
        mockMvc.perform(get("/api/payment-configurations/semesters/"
                + testSemester.getId())
                .header("Authorization", authHeader(loginAsParent())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void parent_cannot_change_payment_configuration() throws Exception {
        mockMvc.perform(put("/api/payment-configurations/academic-years/"
                + testAcademicYear.getId())
                .header("Authorization", authHeader(loginAsParent()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(configurationJson("TPB", "TPBank", "1234567890")))
            .andExpect(status().isForbidden());
    }

    @Test
    void transfer_template_requires_student_code() throws Exception {
        mockMvc.perform(put("/api/payment-configurations/academic-years/"
                + testAcademicYear.getId())
                .header("Authorization", authHeader(loginAsAdmin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(configurationJson("TPB", "TPBank", "1234567890")
                    .replace("MFS {studentCode} {semester}", "HOC PHI {semester}")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Nội dung chuyển khoản phải chứa biến {studentCode}"));
    }

    private void putConfiguration(
            String token,
            Long academicYearId,
            String bankCode,
            String bankName,
            String accountNumber) throws Exception {
        mockMvc.perform(put("/api/payment-configurations/academic-years/" + academicYearId)
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(configurationJson(bankCode, bankName, accountNumber)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.academicYearId").value(academicYearId.intValue()));
    }

    private String configurationJson(
            String bankCode, String bankName, String accountNumber) {
        return """
            {
              "bankCode":"%s",
              "bankName":"%s",
              "accountNumber":"%s",
              "accountHolder":"FPT SCHOOLS",
              "branch":"Ha Noi",
              "transferContentTemplate":"MFS {studentCode} {semester}",
              "enabled":true
            }
            """.formatted(bankCode, bankName, accountNumber);
    }

    private AcademicYear createOtherAcademicYear() {
        AcademicYear year = new AcademicYear();
        year.setName("2027-2028");
        year.setStartDate(LocalDate.of(2027, 8, 1));
        year.setEndDate(LocalDate.of(2028, 5, 31));
        year.setStatus(AcademicYearStatus.DRAFT);
        return academicYearRepository.save(year);
    }

    private Semester createSemester(AcademicYear year) {
        Semester semester = new Semester();
        semester.setName("HK I");
        semester.setOrder(1);
        semester.setStartDate(LocalDate.of(2027, 9, 1));
        semester.setEndDate(LocalDate.of(2028, 1, 15));
        semester.setIsCurrent(false);
        semester.setAcademicYear(year);
        return semesterRepository.save(semester);
    }
}
