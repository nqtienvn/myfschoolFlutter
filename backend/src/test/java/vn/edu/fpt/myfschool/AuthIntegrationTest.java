package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    void register_parent_success() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0901000001","password":"test1234","name":"Phụ huynh Test","role":"PARENT"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andExpect(jsonPath("$.data.user.role").value("PARENT"))
            .andExpect(jsonPath("$.data.user.name").value("Phụ huynh Test"));
    }

    @Test
    void register_student_success() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0901000002","password":"test1234","name":"HS Test","role":"STUDENT","studentCode":"12A-99"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.studentProfile.studentCode").value("12A-99"));
    }

    @Test
    void register_teacher_publicEndpoint_forbidden() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0901000003","password":"test1234","name":"GV Test","role":"TEACHER"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_duplicate_phone_fails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0901000004","password":"test1234","name":"User1","role":"PARENT"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0901000004","password":"test1234","name":"User2","role":"PARENT"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_student_without_code_fails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0901000005","password":"test1234","name":"HS No Code","role":"STUDENT"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_success() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0901000010","password":"test1234","name":"Login Test","role":"PARENT"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0901000010","password":"test1234"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andExpect(jsonPath("$.data.user.name").value("Login Test"));
    }

    @Test
    void login_wrong_password_fails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0901000011","password":"test1234","name":"Login Fail","role":"PARENT"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0901000011","password":"wrongpass"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_nonexistent_user_fails() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0999999999","password":"test1234"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void get_profile_authenticated() throws Exception {
        String token = registerUser("0901000020", "test1234", "Profile User", "PARENT", null);

        mockMvc.perform(get("/api/user/profile")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Profile User"))
            .andExpect(jsonPath("$.data.role").value("PARENT"))
            .andExpect(jsonPath("$.data.parentProfile").exists());
    }

    @Test
    void get_profile_unauthenticated_fails() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
            .andExpect(status().isForbidden());
    }

    @Test
    void change_password_success() throws Exception {
        String token = registerUser("0901000021", "oldpass123", "Change PW User", "PARENT", null);

        mockMvc.perform(put("/api/user/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"oldPassword":"oldpass123","newPassword":"newpass123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify new password works
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0901000021","password":"newpass123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void change_password_wrong_old_fails() throws Exception {
        String token = registerUser("0901000022", "correct123", "Wrong PW", "PARENT", null);

        mockMvc.perform(put("/api/user/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"oldPassword":"wrongpass","newPassword":"newpass123"}
                    """))
            .andExpect(status().isBadRequest());
    }
}
