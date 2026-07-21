package vn.edu.fpt.myfschool.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.config.MailProperties;
import vn.edu.fpt.myfschool.service.MailGateway;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "GMAIL")
public class GmailMailGateway implements MailGateway {
    private static final URI TOKEN_URI = URI.create("https://oauth2.googleapis.com/token");
    private static final URI SEND_URI = URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send");

    private final MailProperties properties;
    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private volatile String accessToken;
    private volatile Instant accessTokenExpiresAt = Instant.EPOCH;

    public GmailMailGateway(
            MailProperties properties,
            ObjectMapper objectMapper,
            @Value("${app.mail.google-client-id:}") String clientId,
            @Value("${app.mail.google-client-secret:}") String clientSecret,
            @Value("${app.mail.google-refresh-token:}") String refreshToken) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetLink) {
        String text = "Bạn vừa yêu cầu đặt lại mật khẩu MyFschool. Link có hiệu lực 15 phút:\n" + resetLink
                + "\nNếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.";
        String html = "<p>Bạn vừa yêu cầu đặt lại mật khẩu MyFschool.</p>"
                + "<p><a href=\"" + escape(resetLink) + "\">Đặt lại mật khẩu</a> (hiệu lực 15 phút).</p>"
                + "<p>Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.</p>";
        send(email, "Đặt lại mật khẩu MyFschool", text, html);
    }

    @Override
    public void sendAccountCreatedEmail(String email, String name, String username,
                                        String temporaryPassword, UserRole role) {
        String roleName = switch (role) {
            case PARENT -> "Phụ huynh";
            case STUDENT -> "Học sinh";
            case TEACHER -> "Giáo viên";
            case ADMIN -> "Quản trị viên";
        };
        String text = "Xin chào " + name + ",\nTài khoản " + roleName + " MyFschool đã được tạo."
                + "\nTên đăng nhập: " + username + "\nMật khẩu tạm: " + temporaryPassword
                + "\nVui lòng đăng nhập và đổi mật khẩu ngay.";
        String html = "<p>Xin chào " + escape(name) + ",</p><p>Tài khoản <strong>" + roleName
                + "</strong> MyFschool đã được tạo.</p><p>Tên đăng nhập: <strong>" + escape(username)
                + "</strong><br>Mật khẩu tạm: <strong>" + escape(temporaryPassword)
                + "</strong></p><p>Vui lòng đăng nhập và đổi mật khẩu ngay.</p>";
        send(email, "Tài khoản MyFschool của bạn", text, html);
    }

    @Override
    public void sendPasswordChangedEmail(String email, String name) {
        String text = "Xin chào " + name + ",\nMật khẩu tài khoản MyFschool của bạn vừa được thay đổi."
                + "\nNếu không phải bạn thực hiện, hãy liên hệ nhà trường ngay.";
        String html = "<p>Xin chào " + escape(name) + ",</p>"
                + "<p>Mật khẩu tài khoản MyFschool của bạn vừa được thay đổi.</p>"
                + "<p>Nếu không phải bạn thực hiện, hãy liên hệ nhà trường ngay.</p>";
        send(email, "Mật khẩu MyFschool vừa được thay đổi", text, html);
    }

    private void send(String recipient, String subject, String plainText, String html) {
        requireConfiguration();
        try {
            String boundary = "myfschool-" + System.nanoTime();
            String mime = "From: " + properties.getFrom() + "\r\n"
                    + "To: " + recipient + "\r\n"
                    + "Subject: " + encodedSubject(subject) + "\r\n"
                    + "MIME-Version: 1.0\r\n"
                    + "Content-Type: multipart/alternative; boundary=\"" + boundary + "\"\r\n\r\n"
                    + "--" + boundary + "\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n" + plainText + "\r\n"
                    + "--" + boundary + "\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n" + html + "\r\n"
                    + "--" + boundary + "--";
            String raw = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mime.getBytes(StandardCharsets.UTF_8));
            String body = objectMapper.writeValueAsString(Map.of("raw", raw));
            HttpRequest request = HttpRequest.newBuilder(SEND_URI)
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + accessToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Gmail API returned HTTP " + response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gmail delivery interrupted", exception);
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("Gmail delivery failed", exception);
        }
    }

    private synchronized String accessToken() throws Exception {
        if (accessToken != null && Instant.now().isBefore(accessTokenExpiresAt.minusSeconds(30))) return accessToken;
        String form = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&refresh_token=" + encode(refreshToken)
                + "&grant_type=refresh_token";
        HttpRequest request = HttpRequest.newBuilder(TOKEN_URI)
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Google OAuth returned HTTP " + response.statusCode());
        }
        JsonNode json = objectMapper.readTree(response.body());
        accessToken = json.path("access_token").asText();
        if (accessToken.isBlank()) throw new IllegalStateException("Google OAuth response has no access token");
        accessTokenExpiresAt = Instant.now().plusSeconds(json.path("expires_in").asLong(3600));
        return accessToken;
    }

    private void requireConfiguration() {
        if (clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank() || properties.getFrom().isBlank()) {
            throw new IllegalStateException("Gmail OAuth configuration is incomplete");
        }
    }

    private static String encodedSubject(String value) {
        return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)) + "?=";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
