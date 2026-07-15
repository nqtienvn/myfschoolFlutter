package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.BillStatus;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.Enrollment;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.TuitionBill;
import vn.edu.fpt.myfschool.repository.TuitionBillRepository;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TuitionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TuitionBillRepository tuitionBillRepository;

    @BeforeEach
    void assignHomeroomTeacher() {
        HomeroomAssignment assignment = new HomeroomAssignment();
        assignment.setCls(testClass);
        assignment.setTeacher(testTeacher);
        assignment.setAcademicYear(testAcademicYear);
        assignment.setEffectiveFrom(testAcademicYear.getStartDate());
        homeroomAssignmentRepository.save(assignment);
    }

    private String billJson(Long studentId, Long classId, Long semesterId, String name, long amount) {
        return billJson(studentId, classId, semesterId, name, amount, "2026-12-31");
    }

    private String billJson(Long studentId, Long classId, Long semesterId, String name,
                            long amount, String dueDate) {
        return "{\"studentId\":" + studentId + ",\"classId\":" + classId + ",\"semesterId\":" + semesterId
            + ",\"name\":\"" + name + "\",\"amount\":" + amount + ",\"dueDate\":\"" + dueDate + "\"}";
    }

    private Semester createSemesterInAnotherAcademicYear() {
        AcademicYear otherYear = new AcademicYear();
        otherYear.setName("2027-2028");
        otherYear.setStartDate(LocalDate.of(2027, 8, 1));
        otherYear.setEndDate(LocalDate.of(2028, 5, 31));
        otherYear.setStatus(AcademicYearStatus.DRAFT);
        otherYear = academicYearRepository.save(otherYear);

        Semester otherSemester = new Semester();
        otherSemester.setName("HK I");
        otherSemester.setOrder(1);
        otherSemester.setStartDate(LocalDate.of(2027, 9, 1));
        otherSemester.setEndDate(LocalDate.of(2028, 1, 15));
        otherSemester.setIsCurrent(false);
        otherSemester.setAcademicYear(otherYear);
        return semesterRepository.save(otherSemester);
    }

    private SchoolClass createOtherClassInTestYear() {
        SchoolClass otherClass = new SchoolClass();
        otherClass.setName("12B");
        otherClass.setGradeLevel(12);
        otherClass.setAcademicYear(testAcademicYear);
        otherClass.setSchoolName("FPT Schools");
        return classRepository.save(otherClass);
    }

    private String feeCategoryJson(String name) {
        return "{\"name\":\"" + name + "\",\"description\":\"Khoan thu bat buoc\"}";
    }

    private String feeTemplateJson(Long feeCategoryId, String name) {
        return "{\"feeCategoryId\":" + feeCategoryId + ",\"classId\":" + testClass.getId()
            + ",\"semesterId\":" + testSemester.getId() + ",\"name\":\"" + name
            + "\",\"amount\":15000000,\"dueDate\":\"2026-12-31\"}";
    }

    private Long createFeeCategory(String token, String name) throws Exception {
        var result = mockMvc.perform(post("/api/fee-categories")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(feeCategoryJson(name)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private Long createFeeTemplate(String token, Long feeCategoryId, String name) throws Exception {
        var result = mockMvc.perform(post("/api/fee-templates")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(feeTemplateJson(feeCategoryId, name)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private Long createBill(Long studentId, String name, long amount) throws Exception {
        var result = mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(loginAsAdmin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(studentId, testClass.getId(), testSemester.getId(), name, amount)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data").path("id").asLong();
    }

    private void configureBankTransfer() throws Exception {
        mockMvc.perform(put("/api/payment-configurations/academic-years/"
                + testAcademicYear.getId())
                .header("Authorization", authHeader(loginAsAdmin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "bankCode":"TPB",
                      "bankName":"TPBank",
                      "accountNumber":"1234567890",
                      "accountHolder":"FPT SCHOOLS",
                      "branch":"Ha Noi",
                      "transferContentTemplate":"MFS {studentCode} {semester}",
                      "enabled":true
                    }
                    """))
            .andExpect(status().isOk());
    }

    @Test
    void create_tuition_bill_admin_only() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(testStudent1.getId(), testClass.getId(), testSemester.getId(), "Hoc phi HK1", 15000000)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Hoc phi HK1"))
            .andExpect(jsonPath("$.data.status").value("UNPAID"));
    }

    @Test
    void create_tuition_bill_parent_forbidden() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(testStudent1.getId(), testClass.getId(), testSemester.getId(), "Test", 1000000)))
            .andExpect(status().isForbidden());
    }

    @Test
    void create_bill_rejects_class_and_semester_from_different_years() throws Exception {
        Semester otherSemester = createSemesterInAnotherAcademicYear();

        mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(loginAsAdmin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(testStudent1.getId(), testClass.getId(), otherSemester.getId(),
                    "Sai nam hoc", 1000000, "2027-12-31")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void create_bill_rejects_student_not_enrolled_in_selected_class() throws Exception {
        SchoolClass otherClass = createOtherClassInTestYear();

        mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(loginAsAdmin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(testStudent1.getId(), otherClass.getId(), testSemester.getId(),
                    "Sai lop", 1000000)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void create_bill_rejects_due_date_outside_selected_semester() throws Exception {
        mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(loginAsAdmin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(testStudent1.getId(), testClass.getId(), testSemester.getId(),
                    "Sai han", 1000000, "2026-08-31")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void teacher_view_class_bills() throws Exception {
        String adminToken = loginAsAdmin();
        String teacherToken = loginAsTeacher();

        mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(testStudent1.getId(), testClass.getId(), testSemester.getId(), "HP List", 10000000)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/tuition/bills/class")
                .header("Authorization", authHeader(teacherToken))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].status").value("UNPAID"));
    }

    @Test
    void non_homeroom_teacher_cannot_view_class_bills() throws Exception {
        homeroomAssignmentRepository.deleteAll();
        String teacherToken = loginAsTeacher();

        mockMvc.perform(get("/api/tuition/bills/class")
                .header("Authorization", authHeader(teacherToken))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    void teacher_cannot_view_bills_when_assignment_does_not_overlap_semester() throws Exception {
        HomeroomAssignment assignment = homeroomAssignmentRepository.findAll().getFirst();
        assignment.setEffectiveTo(testSemester.getStartDate().minusDays(1));
        homeroomAssignmentRepository.saveAndFlush(assignment);

        mockMvc.perform(get("/api/tuition/bills/class")
                .header("Authorization", authHeader(loginAsTeacher()))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tuition/bills/class-summary")
                .header("Authorization", authHeader(loginAsTeacher()))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    void class_list_rejects_class_and_semester_from_different_years() throws Exception {
        Semester otherSemester = createSemesterInAnotherAcademicYear();

        mockMvc.perform(get("/api/tuition/bills/class")
                .header("Authorization", authHeader(loginAsAdmin()))
                .param("classId", testClass.getId().toString())
                .param("semesterId", otherSemester.getId().toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/tuition/bills/class-summary")
                .header("Authorization", authHeader(loginAsTeacher()))
                .param("classId", testClass.getId().toString())
                .param("semesterId", otherSemester.getId().toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void teacher_class_summary_uses_canonical_bills_and_enrollment_roster() throws Exception {
        String adminToken = loginAsAdmin();
        var paidBillResult = mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(testStudent1.getId(), testClass.getId(), testSemester.getId(),
                    "HP da dong", 6000000)))
            .andExpect(status().isOk())
            .andReturn();
        Long paidBillId = objectMapper.readTree(paidBillResult.getResponse().getContentAsString())
            .path("data").path("id").asLong();

        mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(testStudent2.getId(), testClass.getId(), testSemester.getId(),
                    "HP chua dong", 7000000)))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/tuition/bills/" + paidBillId + "/simulate-pay")
                .header("Authorization", authHeader(adminToken)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/tuition/bills/class-summary")
                .header("Authorization", authHeader(loginAsTeacher()))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.classId").value(testClass.getId().intValue()))
            .andExpect(jsonPath("$.data.semesterId").value(testSemester.getId().intValue()))
            .andExpect(jsonPath("$.data.totalStudents").value(3))
            .andExpect(jsonPath("$.data.paidStudents").value(1))
            .andExpect(jsonPath("$.data.outstandingStudents").value(1))
            .andExpect(jsonPath("$.data.studentsWithoutBills").value(1))
            .andExpect(jsonPath("$.data.students[0].studentCode").value(testStudent1.getStudentCode()))
            .andExpect(jsonPath("$.data.students[0].paymentState").value("PAID"))
            .andExpect(jsonPath("$.data.students[0].outstandingAmount").value(0))
            .andExpect(jsonPath("$.data.students[0].bills[0].transactions[0].status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.students[1].studentCode").value(testStudent2.getStudentCode()))
            .andExpect(jsonPath("$.data.students[1].paymentState").value("UNPAID"))
            .andExpect(jsonPath("$.data.students[1].outstandingAmount").value(7000000))
            .andExpect(jsonPath("$.data.students[2].paymentState").value("NO_BILLS"));
    }

    @Test
    void teacher_class_summary_keeps_processing_bill_outstanding() throws Exception {
        var billResult = mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(loginAsAdmin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(testStudent1.getId(), testClass.getId(), testSemester.getId(),
                    "HP dang xu ly", 4500000)))
            .andExpect(status().isOk())
            .andReturn();
        Long billId = objectMapper.readTree(billResult.getResponse().getContentAsString())
            .path("data").path("id").asLong();

        var processingBill = tuitionBillRepository.findById(billId).orElseThrow();
        processingBill.setStatus(BillStatus.PROCESSING);
        tuitionBillRepository.saveAndFlush(processingBill);

        mockMvc.perform(get("/api/tuition/bills/class-summary")
                .header("Authorization", authHeader(loginAsTeacher()))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalStudents").value(3))
            .andExpect(jsonPath("$.data.paidStudents").value(0))
            .andExpect(jsonPath("$.data.outstandingStudents").value(1))
            .andExpect(jsonPath("$.data.studentsWithoutBills").value(2))
            .andExpect(jsonPath("$.data.students[0].paymentState").value("PROCESSING"))
            .andExpect(jsonPath("$.data.students[0].outstandingAmount").value(4500000));
    }

    @Test
    void class_summary_is_teacher_only() throws Exception {
        mockMvc.perform(get("/api/tuition/bills/class-summary")
                .header("Authorization", authHeader(loginAsAdmin()))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    void parent_view_student_bills() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(get("/api/tuition/bills/student")
                .header("Authorization", authHeader(token))
                .param("studentId", testStudent1.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void student_views_only_own_bills_in_selected_semester() throws Exception {
        String adminToken = loginAsAdmin();
        mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(testStudent1.getId(), testClass.getId(), testSemester.getId(),
                    "HP mobile", 9000000)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/tuition/bills/student")
                .header("Authorization", authHeader(loginAsStudent1()))
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].name").value("HP mobile"));

        mockMvc.perform(get("/api/tuition/bills/student")
                .header("Authorization", authHeader(loginAsStudent2()))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    void parent_payment_request_updates_canonical_bill_and_teacher_summary() throws Exception {
        configureBankTransfer();
        Long billId = createBill(testStudent1.getId(), "HP chuyen khoan", 9100000);

        mockMvc.perform(post("/api/tuition/bills/" + billId + "/payment-request")
                .header("Authorization", authHeader(loginAsParent())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.paymentMethod").value("BANK_TRANSFER_CONFIRMATION"))
            .andExpect(jsonPath("$.data.transactionRef").isNotEmpty());

        mockMvc.perform(get("/api/tuition/bills/student")
                .header("Authorization", authHeader(loginAsParent()))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].status").value("PROCESSING"))
            .andExpect(jsonPath("$.data[0].transactions[0].status").value("PENDING"));

        mockMvc.perform(get("/api/tuition/bills/class-summary")
                .header("Authorization", authHeader(loginAsTeacher()))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.students[0].paymentState").value("PROCESSING"))
            .andExpect(jsonPath("$.data.students[0].outstandingAmount").value(9100000));
    }

    @Test
    void payment_request_rejects_unrelated_student_and_parent() throws Exception {
        Long billId = createBill(testStudent1.getId(), "HP bao mat", 9200000);

        mockMvc.perform(post("/api/tuition/bills/" + billId + "/payment-request")
                .header("Authorization", authHeader(loginAsStudent2())))
            .andExpect(status().isForbidden());

        var guardianLink = studentGuardianRepository
            .findByStudentIdAndGuardianId(testStudent1.getId(), testParent.getId())
            .orElseThrow();
        studentGuardianRepository.delete(guardianLink);
        studentGuardianRepository.flush();
        mockMvc.perform(post("/api/tuition/bills/" + billId + "/payment-request")
                .header("Authorization", authHeader(loginAsParent())))
            .andExpect(status().isForbidden());
    }

    @Test
    void payment_request_rejects_duplicate_and_paid_bill() throws Exception {
        configureBankTransfer();
        Long processingBillId = createBill(testStudent1.getId(), "HP gui trung", 9300000);
        String parentToken = loginAsParent();
        mockMvc.perform(post("/api/tuition/bills/" + processingBillId + "/payment-request")
                .header("Authorization", authHeader(parentToken)))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/tuition/bills/" + processingBillId + "/payment-request")
                .header("Authorization", authHeader(parentToken)))
            .andExpect(status().isConflict());

        Long paidBillId = createBill(testStudent1.getId(), "HP da thanh toan", 9400000);
        mockMvc.perform(post("/api/tuition/bills/" + paidBillId + "/simulate-pay")
                .header("Authorization", authHeader(loginAsAdmin())))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/tuition/bills/" + paidBillId + "/payment-request")
                .header("Authorization", authHeader(parentToken)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void payment_request_rejects_when_bank_transfer_is_not_configured() throws Exception {
        Long billId = createBill(testStudent1.getId(), "HP chua cau hinh", 9500000);

        mockMvc.perform(post("/api/tuition/bills/" + billId + "/payment-request")
                .header("Authorization", authHeader(loginAsParent())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(
                "Nhà trường chưa kích hoạt tài khoản nhận chuyển khoản cho năm học này"));
    }

    @Test
    void admin_payment_requests_are_isolated_by_selected_academic_year() throws Exception {
        Semester otherSemester = createSemesterInAnotherAcademicYear();
        AcademicYear otherYear = otherSemester.getAcademicYear();
        SchoolClass otherClass = new SchoolClass();
        otherClass.setName("12C");
        otherClass.setGradeLevel(12);
        otherClass.setAcademicYear(otherYear);
        otherClass.setSchoolName("FPT Schools");
        otherClass = classRepository.save(otherClass);

        Enrollment otherEnrollment = new Enrollment();
        otherEnrollment.setStudent(testStudent1);
        otherEnrollment.setCls(otherClass);
        otherEnrollment.setAcademicYear(otherYear);
        otherEnrollment.setJoinDate(otherYear.getStartDate());
        otherEnrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollmentRepository.save(otherEnrollment);

        createProcessingBill(
            testClass, testSemester, "Doi soat nam hien tai", 5100000);
        createProcessingBill(
            otherClass, otherSemester, "Doi soat nam khac", 5200000);
        String adminToken = loginAsAdmin();

        mockMvc.perform(get("/api/tuition/payment-requests")
                .header("Authorization", authHeader(adminToken))
                .param("academicYearId", testAcademicYear.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].name").value("Doi soat nam hien tai"));

        mockMvc.perform(get("/api/tuition/payment-requests")
                .header("Authorization", authHeader(adminToken))
                .param("academicYearId", otherYear.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].name").value("Doi soat nam khac"));
    }

    @Test
    void admin_can_confirm_or_reject_pending_bank_transfer() throws Exception {
        configureBankTransfer();
        Long confirmedBillId = createBill(testStudent1.getId(), "HP da nhan", 6100000);
        Long rejectedBillId = createBill(testStudent1.getId(), "HP khong thay", 6200000);
        String parentToken = loginAsParent();
        for (Long billId : new Long[] {confirmedBillId, rejectedBillId}) {
            mockMvc.perform(post("/api/tuition/bills/" + billId + "/payment-request")
                    .header("Authorization", authHeader(parentToken)))
                .andExpect(status().isOk());
        }
        String adminToken = loginAsAdmin();

        mockMvc.perform(post("/api/tuition/bills/" + confirmedBillId + "/confirm-payment")
                .header("Authorization", authHeader(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        mockMvc.perform(post("/api/tuition/bills/" + rejectedBillId + "/reject-payment")
                .header("Authorization", authHeader(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("FAILED"));

        org.assertj.core.api.Assertions.assertThat(
            tuitionBillRepository.findById(confirmedBillId).orElseThrow().getStatus())
            .isEqualTo(BillStatus.PAID);
        org.assertj.core.api.Assertions.assertThat(
            tuitionBillRepository.findById(rejectedBillId).orElseThrow().getStatus())
            .isEqualTo(BillStatus.UNPAID);
    }

    private TuitionBill createProcessingBill(
            SchoolClass cls, Semester semester, String name, long amount) {
        TuitionBill bill = new TuitionBill();
        bill.setStudent(testStudent1);
        bill.setCls(cls);
        bill.setSemester(semester);
        bill.setName(name);
        bill.setAmount(java.math.BigDecimal.valueOf(amount));
        bill.setDueDate(semester.getEndDate());
        bill.setStatus(BillStatus.PROCESSING);
        return tuitionBillRepository.save(bill);
    }

    @Test
    void create_fee_category_admin_only() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/fee-categories")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(feeCategoryJson("Hoc phi")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Hoc phi"));
    }

    @Test
    void create_fee_template_counts_seeded_students() throws Exception {
        String token = loginAsAdmin();
        Long categoryId = createFeeCategory(token, "Hoc phi");

        mockMvc.perform(post("/api/fee-templates")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(feeTemplateJson(categoryId, "Hoc phi HK1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.classId").value(testClass.getId().intValue()))
            .andExpect(jsonPath("$.data.semesterId").value(testSemester.getId().intValue()))
            .andExpect(jsonPath("$.data.studentCount").value(3));
    }

    @Test
    void generate_bills_creates_once_then_skips_duplicates() throws Exception {
        String token = loginAsAdmin();
        Long categoryId = createFeeCategory(token, "Hoc phi");
        Long templateId = createFeeTemplate(token, categoryId, "Hoc phi HK1");

        mockMvc.perform(post("/api/fee-templates/" + templateId + "/generate")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalStudents").value(3))
            .andExpect(jsonPath("$.data.created").value(3))
            .andExpect(jsonPath("$.data.skipped").value(0));

        mockMvc.perform(post("/api/fee-templates/" + templateId + "/generate")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalStudents").value(3))
            .andExpect(jsonPath("$.data.created").value(0))
            .andExpect(jsonPath("$.data.skipped").value(3));

        mockMvc.perform(get("/api/tuition/bills/class")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].feeTemplateId").value(templateId.intValue()));
    }

    @Test
    void duplicate_fee_template_fails() throws Exception {
        String token = loginAsAdmin();
        Long categoryId = createFeeCategory(token, "Hoc phi");
        createFeeTemplate(token, categoryId, "Hoc phi HK1");

        mockMvc.perform(post("/api/fee-templates")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(feeTemplateJson(categoryId, "Hoc phi HK1 duplicate")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void simulate_payment() throws Exception {
        String token = loginAsAdmin();

        var result = mockMvc.perform(post("/api/tuition/bills")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(billJson(testStudent1.getId(), testClass.getId(), testSemester.getId(), "HP Pay", 8000000)))
            .andExpect(status().isOk())
            .andReturn();

        Long billId = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(result.getResponse().getContentAsString())
            .get("data").get("id").asLong();

        mockMvc.perform(post("/api/tuition/bills/" + billId + "/simulate-pay")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }
}
