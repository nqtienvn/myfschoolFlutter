package vn.edu.fpt.myfschool;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.AnnouncementReadRepository;
import vn.edu.fpt.myfschool.repository.NotificationRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnnouncementFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired AnnouncementReadRepository announcementReadRepository;
    @Autowired NotificationRepository notificationRepository;

    @Test
    void valid_teacher_announcement_is_published_immediately() throws Exception {
        Long id = createTeacherAnnouncement("Thong bao lop", "Noi dung hop le", "ALL", null);

        assertEquals(4, announcementReadRepository.countByAnnouncementId(id));
        mockMvc.perform(get("/api/announcements/mine")
                        .header("Authorization", authHeader(loginAsTeacher()))
                        .param("academicYearId", testAcademicYear.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].deliveryStatus").value("PUBLISHED"));
    }

    @Test
    void invalid_content_is_rejected_without_recipients_or_notifications() throws Exception {
        configurePolicy(testAcademicYear.getId(), "mua ngay");

        var result = submitTeacherAnnouncement(
                "Thong bao lop", "Phu huynh MUA—NGAY tai day", "ALL", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("SYSTEM_REJECTED"))
                .andExpect(jsonPath("$.data.message").value(
                        "Thông báo này đã vi phạm câu từ trong chính sách của nhà trường."))
                .andExpect(jsonPath("$.data.violations[0].phrase").value("mua ngay"))
                .andReturn();
        Long id = responseData(result.getResponse().getContentAsString())
                .get("announcement").get("id").asLong();

        assertEquals(0, announcementReadRepository.countByAnnouncementId(id));
        assertEquals(0, notificationRepository.findAll().stream()
                .filter(item -> id.equals(item.getRelatedId()))
                .count());
    }

    @Test
    void contains_rule_respects_word_boundaries() throws Exception {
        configurePolicy(testAcademicYear.getId(), "mua");

        submitTeacherAnnouncement("Thong bao", "Cua hang muaban binh thuong", "STUDENT", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("PUBLISHED"));
    }

    @Test
    void rejected_announcement_can_be_retried_without_overwriting_history() throws Exception {
        configurePolicy(testAcademicYear.getId(), "cam");
        var rejected = submitTeacherAnnouncement("Thong bao cam", "Noi dung", "STUDENT", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("SYSTEM_REJECTED"))
                .andReturn();
        Long rejectedId = responseData(rejected.getResponse().getContentAsString())
                .get("announcement").get("id").asLong();

        var retried = submitTeacherAnnouncement(
                "Thong bao hop le", "Noi dung", "STUDENT", rejectedId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.announcement.retryOfAnnouncementId").value(rejectedId))
                .andReturn();
        Long retriedId = responseData(retried.getResponse().getContentAsString())
                .get("announcement").get("id").asLong();

        assertEquals(0, announcementReadRepository.countByAnnouncementId(rejectedId));
        assertEquals(3, announcementReadRepository.countByAnnouncementId(retriedId));
        mockMvc.perform(get("/api/announcements/mine")
                        .header("Authorization", authHeader(loginAsTeacher()))
                        .param("academicYearId", testAcademicYear.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void replacing_policy_keeps_rejected_history_explainable() throws Exception {
        configurePolicy(testAcademicYear.getId(), "noi dung cam");
        submitTeacherAnnouncement("Thong bao", "Day la noi dung cam", "STUDENT", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("SYSTEM_REJECTED"));

        configurePolicy(testAcademicYear.getId(), "quy tac moi");

        mockMvc.perform(get("/api/announcements/mine")
                        .header("Authorization", authHeader(loginAsTeacher()))
                        .param("academicYearId", testAcademicYear.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].deliveryStatus").value("SYSTEM_REJECTED"))
                .andExpect(jsonPath("$.data[0].violations[0].phrase").value("noi dung cam"));
    }

    @Test
    void policy_is_strictly_scoped_to_selected_academic_year() throws Exception {
        configurePolicy(testAcademicYear.getId(), "noi dung cam");
        AcademicYear otherYear = new AcademicYear();
        otherYear.setName("2030-2031");
        otherYear.setStartDate(LocalDate.of(2030, 8, 1));
        otherYear.setEndDate(LocalDate.of(2031, 5, 31));
        otherYear.setStatus(AcademicYearStatus.DRAFT);
        otherYear = academicYearRepository.save(otherYear);

        mockMvc.perform(get("/api/announcements/admin/policy")
                        .header("Authorization", authHeader(loginAsAdmin()))
                        .param("academicYearId", testAcademicYear.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rules.length()").value(1));
        mockMvc.perform(get("/api/announcements/admin/policy")
                        .header("Authorization", authHeader(loginAsAdmin()))
                        .param("academicYearId", otherYear.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rules.length()").value(0));
    }

    @Test
    void admin_list_is_paginated_and_contains_admin_broadcasts() throws Exception {
        createTeacherAnnouncement("Thong bao lop", "Noi dung", "STUDENT", null);
        createAdminBroadcast("Thong bao toan truong 1");
        createAdminBroadcast("Thong bao toan truong 2");

        mockMvc.perform(get("/api/announcements/admin")
                        .header("Authorization", authHeader(loginAsAdmin()))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("status", "PUBLISHED")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].senderType").value("ADMIN"));
    }

    @Test
    void user_outside_published_audience_cannot_read_announcement() throws Exception {
        Long id = createTeacherAnnouncement("Thong bao", "Noi dung", "PARENT", null);
        String outsiderToken = createOutsideParentAndLogin();

        mockMvc.perform(put("/api/announcements/{id}/read", id)
                        .header("Authorization", authHeader(outsiderToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void another_teacher_cannot_view_recipient_read_tracking() throws Exception {
        Long id = createTeacherAnnouncement("Thong bao", "Noi dung", "ALL", null);

        mockMvc.perform(get("/api/announcements/{id}/recipients", id)
                        .header("Authorization", authHeader(createOtherTeacherAndLogin()))
                        .param("academicYearId", testAcademicYear.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_direct_announcement_reaches_every_non_admin_account() throws Exception {
        createOutsideParentAndLogin();
        createOtherTeacherAndLogin();
        long expectedRecipients = userRepository.findAll().stream()
                .filter(user -> user.getRole() != UserRole.ADMIN).count();

        Long id = createAdminBroadcast("Thong bao toan truong");

        assertEquals(expectedRecipients, announcementReadRepository.countByAnnouncementId(id));
    }

    @Test
    void policy_endpoint_rejects_non_admin_user() throws Exception {
        mockMvc.perform(put("/api/announcements/admin/policy")
                        .header("Authorization", authHeader(loginAsTeacher()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyBody(testAcademicYear.getId(), "cam")))
                .andExpect(status().isForbidden());
    }

    private Long createTeacherAnnouncement(String title, String body,
            String targetRole, Long retryOfAnnouncementId) throws Exception {
        var result = submitTeacherAnnouncement(title, body, targetRole, retryOfAnnouncementId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("PUBLISHED"))
                .andReturn();
        return responseData(result.getResponse().getContentAsString())
                .get("announcement").get("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions submitTeacherAnnouncement(
            String title, String body, String targetRole, Long retryOfAnnouncementId) throws Exception {
        String retry = retryOfAnnouncementId == null ? ""
                : ",\"retryOfAnnouncementId\":" + retryOfAnnouncementId;
        return mockMvc.perform(post("/api/announcements")
                .header("Authorization", authHeader(loginAsTeacher()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"body\":\"" + body
                        + "\",\"targetRole\":\"" + targetRole + "\",\"academicYearId\":"
                        + testAcademicYear.getId() + ",\"classIds\":[" + testClass.getId()
                        + "]" + retry + "}"));
    }

    private void configurePolicy(Long academicYearId, String phrase) throws Exception {
        mockMvc.perform(put("/api/announcements/admin/policy")
                        .header("Authorization", authHeader(loginAsAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyBody(academicYearId, phrase)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rules.length()").value(1));
    }

    private String policyBody(Long academicYearId, String phrase) {
        return "{\"academicYearId\":" + academicYearId
                + ",\"enabled\":true,\"rejectionMessage\":"
                + "\"Thông báo này đã vi phạm câu từ trong chính sách của nhà trường.\","
                + "\"rules\":[{\"phrase\":\"" + phrase
                + "\",\"scope\":\"ALL\",\"matchType\":\"CONTAINS\"}]}";
    }

    private Long createAdminBroadcast(String title) throws Exception {
        var result = mockMvc.perform(post("/api/announcements/admin/broadcast")
                        .header("Authorization", authHeader(loginAsAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title
                                + "\",\"body\":\"Noi dung\",\"academicYearId\":"
                                + testAcademicYear.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deliveryStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.recipientScope").value("SCHOOL"))
                .andReturn();
        return responseData(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private JsonNode responseData(String response) throws Exception {
        return objectMapper.readTree(response).get("data");
    }

    private String createOutsideParentAndLogin() throws Exception {
        User user = new User();
        user.setPhone("0909888001");
        user.setPassword(passwordEncoder.encode("test1234"));
        user.setName("PH Ngoai Lop");
        user.setRole(UserRole.PARENT);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        Parent parent = new Parent();
        parent.setUser(user);
        parentRepository.save(parent);
        return login("0909888001", "test1234");
    }

    private String createOtherTeacherAndLogin() throws Exception {
        User user = new User();
        user.setPhone("0909888002");
        user.setPassword(passwordEncoder.encode("test1234"));
        user.setName("GV Khac");
        user.setRole(UserRole.TEACHER);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setEmployeeCode("GV999");
        teacherRepository.save(teacher);
        return login("0909888002", "test1234");
    }
}
