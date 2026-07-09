package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminRoleIntegrationTest extends BaseIntegrationTest {

    @Test
    void login_adminUser_returnsAdminRole() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0909000009\",\"password\":\"test1234\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
    }

    @Test
    void listUsers_adminRole_returnsFilteredUsers() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", authHeader(token))
                .param("role", "STUDENT")
                .param("page", "0")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[0].role").value("STUDENT"))
            .andExpect(jsonPath("$.data.totalElements").value(3))
            .andExpect(jsonPath("$.data.size").value(2))
            .andExpect(jsonPath("$.data.number").value(0));
    }

    @Test
    void updateUserStatus_adminRole_locksAndUnlocks() throws Exception {
        String token = loginAsAdmin();
        Long targetId = testStudent1.getUser().getId();

        // Lock
        mockMvc.perform(put("/api/admin/users/" + targetId + "/status")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"LOCKED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("LOCKED"));

        // Unlock
        mockMvc.perform(put("/api/admin/users/" + targetId + "/status")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void updateUserStatus_invalidStatus_returns400() throws Exception {
        String token = loginAsAdmin();
        Long targetId = testStudent1.getUser().getId();

        mockMvc.perform(put("/api/admin/users/" + targetId + "/status")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DELETED\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createTeacherAccount_adminRole_createsTeacherWithDefaultPassword() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/admin/users/teachers")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0909000099\",\"name\":\"GV Manual\",\"employeeCode\":\"GV999\",\"department\":\"Toán\",\"subjectIds\":[" + testSubject.getId() + "]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.employeeCode").value("GV999"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.subjects[0].name").value("Toán"));

        login("0909000099", "12345678");
    }

    @Test
    void createTeacherAccount_teacherRole_returns403() throws Exception {
        String token = loginAsTeacher();

        mockMvc.perform(post("/api/admin/users/teachers")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0909000098\",\"name\":\"GV Fail\",\"employeeCode\":\"GV998\",\"subjectIds\":[" + testSubject.getId() + "]}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_teacherRole_returns403() throws Exception {
        String token = loginAsTeacher();

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_parentRole_returns403() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isForbidden());
    }
}
