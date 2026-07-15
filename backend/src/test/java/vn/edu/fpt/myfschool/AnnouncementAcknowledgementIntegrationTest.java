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
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.AnnouncementReadRepository;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnnouncementAcknowledgementIntegrationTest extends BaseIntegrationTest {

    @Autowired AnnouncementReadRepository announcementReadRepository;

    @Test
    void pending_announcement_has_no_recipients_and_approval_creates_year_scoped_snapshot() throws Exception {
        Long id = createTeacherAnnouncement("ALL", true);
        String adminToken = loginAsAdmin();

        mockMvc.perform(get("/api/announcements/{id}/recipients", id)
                        .header("Authorization", authHeader(adminToken))
                        .param("academicYearId", testAcademicYear.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        approve(id);

        mockMvc.perform(get("/api/announcements/{id}/recipients", id)
                        .header("Authorization", authHeader(adminToken))
                        .param("academicYearId", testAcademicYear.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(4));

        org.junit.jupiter.api.Assertions.assertEquals(4, announcementReadRepository.countByAnnouncementId(id));
    }

    @Test
    void user_outside_snapshot_cannot_read_acknowledge_or_reply() throws Exception {
        Long id = createTeacherAnnouncement("PARENT", true);
        approve(id);
        String outsiderToken = createOutsideParentAndLogin();

        mockMvc.perform(put("/api/announcements/{id}/read", id)
                        .header("Authorization", authHeader(outsiderToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/announcements/{id}/acknowledge", id)
                        .header("Authorization", authHeader(outsiderToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/announcements/{id}/reply", id)
                        .header("Authorization", authHeader(outsiderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"replyText\":\"Ngoai lop\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void another_teacher_cannot_view_tracking() throws Exception {
        Long id = createTeacherAnnouncement("ALL", true);
        approve(id);
        String otherTeacherToken = createOtherTeacherAndLogin();

        mockMvc.perform(get("/api/announcements/{id}/recipients", id)
                        .header("Authorization", authHeader(otherTeacherToken))
                        .param("academicYearId", testAcademicYear.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void repeated_open_counts_once_and_reply_also_acknowledges() throws Exception {
        Long id = createTeacherAnnouncement("STUDENT", true);
        approve(id);
        String studentToken = loginAsStudent1();

        mockMvc.perform(get("/api/announcements/{id}", id)
                        .header("Authorization", authHeader(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true));
        mockMvc.perform(get("/api/announcements/{id}", id)
                        .header("Authorization", authHeader(studentToken)))
                .andExpect(status().isOk());

        String teacherToken = loginAsTeacher();
        mockMvc.perform(get("/api/announcements/{id}/recipients", id)
                        .header("Authorization", authHeader(teacherToken))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("status", "READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/announcements/pending-action-count")
                        .header("Authorization", authHeader(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        mockMvc.perform(put("/api/announcements/{id}/reply", id)
                        .header("Authorization", authHeader(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"replyText\":\"Em da nhan thong bao\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/announcements/{id}/recipients", id)
                        .header("Authorization", authHeader(teacherToken))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("role", "STUDENT")
                        .param("status", "REPLIED")
                        .param("keyword", "HS 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].replyText").value("Em da nhan thong bao"))
                .andExpect(jsonPath("$.data.content[0].acknowledgedAt").isNotEmpty());

        mockMvc.perform(get("/api/announcements/pending-action-count")
                        .header("Authorization", authHeader(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    void recipient_tracking_rejects_another_academic_year_scope() throws Exception {
        Long id = createTeacherAnnouncement("ALL", true);
        approve(id);
        AcademicYear otherYear = new AcademicYear();
        otherYear.setName("2029-2030");
        otherYear.setStartDate(LocalDate.of(2029, 8, 1));
        otherYear.setEndDate(LocalDate.of(2030, 5, 31));
        otherYear.setStatus(AcademicYearStatus.DRAFT);
        otherYear = academicYearRepository.save(otherYear);

        mockMvc.perform(get("/api/announcements/{id}/recipients", id)
                        .header("Authorization", authHeader(loginAsAdmin()))
                        .param("academicYearId", otherYear.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_direct_announcement_creates_snapshot_only_for_selected_year_class() throws Exception {
        String adminToken = loginAsAdmin();
        var result = mockMvc.perform(post("/api/announcements/admin/broadcast")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Xac nhan\",\"body\":\"Noi dung\",\"academicYearId\":"
                                + testAcademicYear.getId() + ",\"recipientScope\":\"CLASSES\",\"targetRole\":\"PARENT\","
                                + "\"requiresReply\":true,\"classIds\":[" + testClass.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRecipients").value(1))
                .andReturn();
        Long id = responseData(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/announcements/{id}/recipients", id)
                        .header("Authorization", authHeader(adminToken))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("classId", testClass.getId().toString())
                        .param("role", "PARENT")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    private Long createTeacherAnnouncement(String targetRole, boolean requiresReply) throws Exception {
        var result = mockMvc.perform(post("/api/announcements")
                        .header("Authorization", authHeader(loginAsTeacher()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Thong bao can xac nhan\",\"body\":\"Noi dung\",\"targetRole\":\""
                                + targetRole + "\",\"requiresReply\":" + requiresReply + ",\"academicYearId\":"
                                + testAcademicYear.getId() + ",\"classIds\":[" + testClass.getId() + "]}"))
                .andExpect(status().isOk())
                .andReturn();
        return responseData(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void approve(Long id) throws Exception {
        mockMvc.perform(put("/api/announcements/{id}/review", id)
                        .header("Authorization", authHeader(loginAsAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approve\":true}"))
                .andExpect(status().isOk());
    }

    private JsonNode responseData(String response) throws Exception {
        return objectMapper.readTree(response).get("data");
    }

    private String createOutsideParentAndLogin() throws Exception {
        User user = new User();
        user.setPhone("0909888001"); user.setPassword(passwordEncoder.encode("test1234"));
        user.setName("PH Ngoai Lop"); user.setRole(UserRole.PARENT); user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        Parent parent = new Parent(); parent.setUser(user); parentRepository.save(parent);
        return login("0909888001", "test1234");
    }

    private String createOtherTeacherAndLogin() throws Exception {
        User user = new User();
        user.setPhone("0909888002"); user.setPassword(passwordEncoder.encode("test1234"));
        user.setName("GV Khac"); user.setRole(UserRole.TEACHER); user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        Teacher teacher = new Teacher(); teacher.setUser(user); teacher.setEmployeeCode("GV999");
        teacherRepository.save(teacher);
        return login("0909888002", "test1234");
    }
}
