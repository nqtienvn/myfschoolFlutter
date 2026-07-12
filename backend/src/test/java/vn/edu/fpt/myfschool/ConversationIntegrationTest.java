package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ConversationIntegrationTest extends BaseIntegrationTest {

    @Test
    void create_conversation_between_parent_and_teacher() throws Exception {
        String parentToken = loginAsParent();
        String teacherToken = loginAsTeacher();

        var profileResult = mockMvc.perform(get("/api/user/profile").header("Authorization", authHeader(teacherToken)))
            .andExpect(status().isOk()).andReturn();
        Long teacherUserId = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(profileResult.getResponse().getContentAsString())
            .get("data").get("id").asLong();

        mockMvc.perform(post("/api/conversations")
                .header("Authorization", authHeader(parentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"otherUserId\":" + teacherUserId + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    void list_conversations_empty() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(get("/api/conversations").header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void unread_count_initially_zero() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(get("/api/conversations/unread-count").header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    void create_announcement_teacher_only() throws Exception {
        String token = loginAsTeacher();

        mockMvc.perform(post("/api/announcements")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Thong bao lich thi\",\"body\":\"Lich thi cuoi ky\",\"targetRole\":\"ALL\",\"requiresReply\":false,\"classIds\":[" + testClass.getId() + "]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("Thong bao lich thi"));
    }

    @Test
    void create_announcement_rejects_class_from_another_academic_year() throws Exception {
        AcademicYear otherYear = new AcademicYear();
        otherYear.setName("2027-2028");
        otherYear.setStartDate(LocalDate.of(2027, 8, 1));
        otherYear.setEndDate(LocalDate.of(2028, 5, 31));
        otherYear.setStatus(AcademicYearStatus.DRAFT);
        otherYear = academicYearRepository.save(otherYear);
        SchoolClass otherClass = new SchoolClass();
        otherClass.setName("10Z"); otherClass.setGradeLevel(10); otherClass.setAcademicYear(otherYear);
        otherClass = classRepository.save(otherClass);

        String token = loginAsTeacher();
        mockMvc.perform(post("/api/announcements")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Cross year\",\"body\":\"Blocked\",\"targetRole\":\"ALL\",\"academicYearId\":"
                        + otherYear.getId() + ",\"classIds\":[" + otherClass.getId() + "]}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void admin_can_broadcast_announcement_without_teacher() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/announcements/admin/broadcast")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Thong bao toan truong\",\"body\":\"Noi dung\",\"academicYearId\":"
                        + testAcademicYear.getId() + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.senderType").value("ADMIN"))
            .andExpect(jsonPath("$.data.teacherId").doesNotExist());
    }

    @Test
    void create_announcement_parent_forbidden() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(post("/api/announcements")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test\",\"body\":\"Test body\",\"targetRole\":\"ALL\",\"classIds\":[" + testClass.getId() + "]}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void list_notifications_empty() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(get("/api/notifications").header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void notification_unread_count_zero() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    void mark_all_notifications_read() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(put("/api/notifications/read-all").header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
