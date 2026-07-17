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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnnouncementFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired AnnouncementReadRepository announcementReadRepository;

    @Test
    void teacher_announcement_is_published_only_after_admin_approval() throws Exception {
        Long id = createTeacherAnnouncement("ALL");

        assertEquals(0, announcementReadRepository.countByAnnouncementId(id));

        approve(id);

        assertEquals(4, announcementReadRepository.countByAnnouncementId(id));
    }

    @Test
    void user_outside_published_audience_cannot_read_announcement() throws Exception {
        Long id = createTeacherAnnouncement("PARENT");
        approve(id);
        String outsiderToken = createOutsideParentAndLogin();

        mockMvc.perform(put("/api/announcements/{id}/read", id)
                        .header("Authorization", authHeader(outsiderToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void another_teacher_cannot_view_recipient_read_tracking() throws Exception {
        Long id = createTeacherAnnouncement("ALL");
        approve(id);
        String otherTeacherToken = createOtherTeacherAndLogin();

        mockMvc.perform(get("/api/announcements/{id}/recipients", id)
                        .header("Authorization", authHeader(otherTeacherToken))
                        .param("academicYearId", testAcademicYear.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void opening_announcement_twice_records_one_read() throws Exception {
        Long id = createTeacherAnnouncement("STUDENT");
        approve(id);
        String studentToken = loginAsStudent1();

        mockMvc.perform(get("/api/announcements/{id}", id)
                        .header("Authorization", authHeader(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true));
        mockMvc.perform(get("/api/announcements/{id}", id)
                        .header("Authorization", authHeader(studentToken)))
                .andExpect(status().isOk());

        assertEquals(1, announcementReadRepository.countByAnnouncementIdAndReadAtIsNotNull(id));

        mockMvc.perform(get("/api/announcements/{id}/recipients", id)
                        .header("Authorization", authHeader(loginAsTeacher()))
                        .param("academicYearId", testAcademicYear.getId().toString())
                        .param("status", "READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void teacher_recipient_tracking_rejects_another_academic_year_scope() throws Exception {
        Long id = createTeacherAnnouncement("ALL");
        approve(id);
        AcademicYear otherYear = new AcademicYear();
        otherYear.setName("2029-2030");
        otherYear.setStartDate(LocalDate.of(2029, 8, 1));
        otherYear.setEndDate(LocalDate.of(2030, 5, 31));
        otherYear.setStatus(AcademicYearStatus.DRAFT);
        otherYear = academicYearRepository.save(otherYear);

        mockMvc.perform(get("/api/announcements/{id}/recipients", id)
                        .header("Authorization", authHeader(loginAsTeacher()))
                        .param("academicYearId", otherYear.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_direct_announcement_reaches_every_non_admin_account() throws Exception {
        createOutsideParentAndLogin();
        createOtherTeacherAndLogin();
        long expectedRecipients = userRepository.findAll().stream()
                .filter(user -> user.getRole() != UserRole.ADMIN)
                .count();
        String adminToken = loginAsAdmin();

        var result = mockMvc.perform(post("/api/announcements/admin/broadcast")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Thong bao toan truong\",\"body\":\"Noi dung\",\"academicYearId\":"
                                + testAcademicYear.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipientScope").value("SCHOOL"))
                .andReturn();
        Long id = responseData(result.getResponse().getContentAsString()).get("id").asLong();

        assertEquals(expectedRecipients, announcementReadRepository.countByAnnouncementId(id));
        mockMvc.perform(get("/api/announcements/{id}/recipients", id)
                        .header("Authorization", authHeader(adminToken))
                        .param("academicYearId", testAcademicYear.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacher_inbox_and_unread_count_are_scoped_to_selected_year() throws Exception {
        mockMvc.perform(post("/api/announcements/admin/broadcast")
                        .header("Authorization", authHeader(loginAsAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Thong bao giao vien\",\"body\":\"Noi dung\",\"academicYearId\":"
                                + testAcademicYear.getId() + "}"))
                .andExpect(status().isOk());
        String teacherToken = loginAsTeacher();

        mockMvc.perform(get("/api/announcements")
                        .header("Authorization", authHeader(teacherToken)))
                .andExpect(status().isBadRequest());
        var inbox = mockMvc.perform(get("/api/announcements")
                        .header("Authorization", authHeader(teacherToken))
                        .param("academicYearId", testAcademicYear.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andReturn();
        Long id = responseData(inbox.getResponse().getContentAsString()).get(0).get("id").asLong();

        mockMvc.perform(get("/api/announcements/unread-count")
                        .header("Authorization", authHeader(teacherToken)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/announcements/unread-count")
                        .header("Authorization", authHeader(teacherToken))
                        .param("academicYearId", testAcademicYear.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
        mockMvc.perform(get("/api/announcements/{id}", id)
                        .header("Authorization", authHeader(teacherToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/announcements/unread-count")
                        .header("Authorization", authHeader(teacherToken))
                        .param("academicYearId", testAcademicYear.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }

    private Long createTeacherAnnouncement(String targetRole) throws Exception {
        var result = mockMvc.perform(post("/api/announcements")
                        .header("Authorization", authHeader(loginAsTeacher()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Thong bao lop\",\"body\":\"Noi dung\",\"targetRole\":\""
                                + targetRole + "\",\"academicYearId\":" + testAcademicYear.getId()
                                + ",\"classIds\":[" + testClass.getId() + "]}"))
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
