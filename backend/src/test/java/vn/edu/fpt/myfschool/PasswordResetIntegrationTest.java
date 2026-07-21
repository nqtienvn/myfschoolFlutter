package vn.edu.fpt.myfschool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.entity.PasswordResetToken;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.AuditLogRepository;
import vn.edu.fpt.myfschool.repository.PasswordResetAttemptRepository;
import vn.edu.fpt.myfschool.repository.PasswordResetTokenRepository;
import vn.edu.fpt.myfschool.repository.UserRepository;
import vn.edu.fpt.myfschool.service.impl.FakeMailGateway;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordResetTokenRepository tokenRepository;
    @Autowired PasswordResetAttemptRepository attemptRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired FakeMailGateway fakeMailGateway;

    @BeforeEach
    void clean() {
        tokenRepository.deleteAll();
        attemptRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        fakeMailGateway.clear();
    }

    @AfterEach
    void cleanAfter() {
        clean();
    }

    @Test
    void verifiedParentGetsHashedSingleUseTokenAndOldJwtIsRevoked() throws Exception {
        User parent = createUser("0901234567", "Parent@Test.Example", UserRole.PARENT, UserStatus.ACTIVE, true);
        String oldJwt = login(parent.getPhone(), "OldPass123");

        requestReset(parent.getPhone()).andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Nếu tài khoản đủ điều kiện, hướng dẫn đặt lại mật khẩu đã được gửi qua email."));

        String rawToken = latestResetToken();
        PasswordResetToken stored = tokenRepository.findAll().getFirst();
        assertNotEquals(rawToken, stored.getTokenHash());
        assertEquals(64, stored.getTokenHash().length());

        validate(rawToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.status").value("VALID"));

        confirm(rawToken, "NewPass123").andExpect(status().isOk());
        assertTrue(passwordEncoder.matches("NewPass123", userRepository.findById(parent.getId()).orElseThrow().getPassword()));
        assertFalse(userRepository.findById(parent.getId()).orElseThrow().getMustChangePassword());
        assertTrue(userRepository.findById(parent.getId()).orElseThrow().getCredentialsUpdatedAt() != null);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0901234567\",\"password\":\"OldPass123\"}"))
                .andExpect(status().isBadRequest());
        login(parent.getPhone(), "NewPass123");
        mockMvc.perform(get("/api/user/profile").header("Authorization", "Bearer " + oldJwt))
                .andExpect(status().isForbidden());

        validate(rawToken).andExpect(jsonPath("$.data.status").value("USED"));
        assertEquals(1, fakeMailGateway.sentMessages().stream()
                .filter(mail -> mail.type().equals("PASSWORD_CHANGED")).count());

        List<String> auditBodies = auditLogRepository.findAll().stream()
                .filter(log -> "/api/auth/password-reset/confirm".equals(log.getUri()))
                .map(log -> log.getRequestBody() == null ? "" : log.getRequestBody())
                .toList();
        assertFalse(auditBodies.isEmpty());
        assertTrue(auditBodies.stream().noneMatch(body -> body.contains(rawToken) || body.contains("NewPass123")));
    }

    @Test
    void genericResponseDoesNotMailMissingOrUnverifiedAccounts() throws Exception {
        createUser("0901234568", "unverified@test.example", UserRole.PARENT, UserStatus.ACTIVE, false);

        String missingMessage = responseMessage(requestReset("0900000000").andReturn());
        String unverifiedMessage = responseMessage(requestReset("0901234568").andReturn());

        assertEquals(missingMessage, unverifiedMessage);
        assertTrue(fakeMailGateway.sentMessages().isEmpty());
    }

    @Test
    void verifiedStudentGetsResetLinkAndCanSetNewPassword() throws Exception {
        User student = createUser("0901234569", "Student@Test.Example", UserRole.STUDENT, UserStatus.ACTIVE, true);

        requestReset(student.getPhone()).andExpect(status().isOk());
        assertEquals(1, fakeMailGateway.sentMessages().stream()
                .filter(mail -> mail.type().equals("PASSWORD_RESET")
                        && mail.recipient().equals("student@test.example"))
                .count());

        String token = latestResetToken();
        confirm(token, "StudentNew123").andExpect(status().isOk());
        User reloaded = userRepository.findById(student.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("StudentNew123", reloaded.getPassword()));
        assertFalse(reloaded.getMustChangePassword());
    }

    @Test
    void newRequestReplacesOldTokenAndTokenCannotResetAnotherUser() throws Exception {
        User first = createUser("0901234570", "first@test.example", UserRole.PARENT, UserStatus.ACTIVE, true);
        User second = createUser("0901234571", "second@test.example", UserRole.TEACHER, UserStatus.ACTIVE, true);

        requestReset(first.getPhone());
        String oldToken = latestResetToken();
        requestReset(first.getPhone());
        String replacement = latestResetToken();

        assertNotEquals(oldToken, replacement);
        validate(oldToken).andExpect(jsonPath("$.data.status").value("USED"));
        confirm(replacement, "FirstNew123").andExpect(status().isOk());
        assertTrue(passwordEncoder.matches("FirstNew123", userRepository.findById(first.getId()).orElseThrow().getPassword()));
        assertTrue(passwordEncoder.matches("OldPass123", userRepository.findById(second.getId()).orElseThrow().getPassword()));

        validate("not-a-real-token").andExpect(jsonPath("$.data.status").value("INVALID"));
    }

    @Test
    void expiredTokenIsRejectedAndLockedAccountStaysLockedAfterReset() throws Exception {
        User locked = createUser("0901234572", "locked@test.example", UserRole.TEACHER, UserStatus.LOCKED, true);
        requestReset(locked.getPhone());
        String expiredRaw = latestResetToken();
        PasswordResetToken token = tokenRepository.findAll().getFirst();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        tokenRepository.save(token);
        validate(expiredRaw).andExpect(jsonPath("$.data.status").value("EXPIRED"));
        confirm(expiredRaw, "NoChange123").andExpect(status().isBadRequest());

        tokenRepository.deleteAll();
        attemptRepository.deleteAll();
        fakeMailGateway.clear();
        requestReset(locked.getPhone());
        confirm(latestResetToken(), "LockedNew123").andExpect(status().isOk());
        User reloaded = userRepository.findById(locked.getId()).orElseThrow();
        assertEquals(UserStatus.LOCKED, reloaded.getStatus());
        assertTrue(passwordEncoder.matches("LockedNew123", reloaded.getPassword()));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0901234572\",\"password\":\"LockedNew123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rateLimitsAccountAndIpWithoutChangingGenericResponse() throws Exception {
        User parent = createUser("0901234573", "limited@test.example", UserRole.PARENT, UserStatus.ACTIVE, true);
        for (int index = 0; index < 4; index++) requestReset(parent.getPhone()).andExpect(status().isOk());
        assertEquals(3, fakeMailGateway.sentMessages().stream()
                .filter(mail -> mail.type().equals("PASSWORD_RESET")).count());

        tokenRepository.deleteAll();
        attemptRepository.deleteAll();
        fakeMailGateway.clear();
        for (int index = 0; index < 11; index++) {
            String phone = "0912%06d".formatted(index);
            createUser(phone, "ip" + index + "@test.example", UserRole.PARENT, UserStatus.ACTIVE, true);
            requestReset(phone).andExpect(status().isOk());
        }
        assertEquals(10, fakeMailGateway.sentMessages().stream()
                .filter(mail -> mail.type().equals("PASSWORD_RESET")).count());
    }

    private User createUser(String phone, String email, UserRole role, UserStatus status, boolean verified) {
        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("OldPass123"));
        user.setName(role.name() + " test");
        user.setEmail(email);
        user.setEmailVerifiedAt(verified ? LocalDateTime.now() : null);
        user.setRole(role);
        user.setStatus(status);
        return userRepository.saveAndFlush(user);
    }

    private org.springframework.test.web.servlet.ResultActions requestReset(String phone) throws Exception {
        return mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + phone + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions validate(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/password-reset/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("token", token))));
    }

    private org.springframework.test.web.servlet.ResultActions confirm(String token, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                        "token", token, "newPassword", password, "confirmPassword", password))));
    }

    private String latestResetToken() {
        return fakeMailGateway.sentMessages().reversed().stream()
                .filter(mail -> mail.type().equals("PASSWORD_RESET"))
                .findFirst().orElseThrow().resetLink().split("#token=", 2)[1];
    }

    private String login(String phone, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + phone + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("token").asText();
    }

    private String responseMessage(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("message").asText();
    }
}
