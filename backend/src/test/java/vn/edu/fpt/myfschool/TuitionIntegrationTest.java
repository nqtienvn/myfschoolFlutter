package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TuitionIntegrationTest extends BaseIntegrationTest {

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
        return "{\"studentId\":" + studentId + ",\"classId\":" + classId + ",\"semesterId\":" + semesterId
            + ",\"name\":\"" + name + "\",\"amount\":" + amount + ",\"dueDate\":\"2026-12-31\"}";
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
