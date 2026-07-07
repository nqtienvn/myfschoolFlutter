# 🔒 SECURITY TESTS – MyFschool Chat Realtime

> **Thư mục:** `06-Testing/SECURITY_TESTS/`  
> **Mục đích:** Hướng dẫn kiểm thử bảo mật cho Module Chat Realtime bao gồm xác thực (Authentication) handshake, phân quyền hội thoại (Authorization/Membership) và an toàn mã hóa dữ liệu truyền tải.  
> **Công cụ:** Newman (Postman API Security), Custom Java Socket Security Test Suite

---

## 1. Ma trận Rủi ro & Kịch bản kiểm thử bảo mật

| Mã lỗi | Rủi ro bảo mật | Mức độ | Kịch bản kiểm thử | Trạng thái |
| :--- | :--- | :---: | :--- | :---: |
| **SEC-CHAT-001** | Kết nối ẩn danh (Missing/Invalid Token) | 🔴 Critical | Handshake không token hoặc token giả mạo bị từ chối | ✅ Passed |
| **SEC-CHAT-002** | Xâm nhập trái phép (Membership Bypass) | 🔴 Critical | User gửi tin nhắn tới cuộc hội thoại không phải của mình | ✅ Passed |
| **SEC-CHAT-003** | Đọc trộm tin nhắn (IDOR Read) | 🔴 Critical | User cố tình GET messages của hội thoại khác qua REST API | ✅ Passed |
| **SEC-CHAT-004** | Tấn công Replay refresh token | 🟠 High | Sử dụng lại refresh token cũ bị thu hồi để lấy JWT mới | ✅ Passed |
| **SEC-CHAT-005** | Leak dữ liệu nhạy cảm qua LOG | 🟡 Medium | Server in plaintext mật khẩu/token hoặc PII của PH/HS | ✅ Passed |

---

## 2. Kịch bản Code Kiểm thử bảo mật (Spring Boot Backend)

#### `ChatSecurityIntegrationTest.java`

```java
/**
 * Security integration tests for Chat WebSocket and REST API.
 *
 * Coverage:
 * - SEC-CHAT-001: Tampered JWT token rejection at handshake
 * - SEC-CHAT-002: Membership bypass prevention (message.send to foreign conversation)
 * - SEC-CHAT-003: IDOR prevention (GET messages of foreign conversation)
 *
 * Related UC: UC08 Real-time Chat
 * Test Case Doc: Test_Cases/UC08_RealtimeChat_TESTCASE.md
 */
@Tag("security")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatSecurityIntegrationTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    // Token mẫu
    private String parentBToken;
    private String strangerCToken;

    @BeforeEach
    void setUp() {
        // Tạo token hợp lệ cho Phụ huynh B (User ID = 10)
        parentBToken = jwtTokenProvider.generateToken(10L, "PARENT");
        // Tạo token hợp lệ cho User C (User ID = 20 - người lạ)
        strangerCToken = jwtTokenProvider.generateToken(20L, "PARENT");
    }

    @Test
    @DisplayName("SEC-CHAT-001: Handshake thất bại khi token bị giả mạo")
    void testHandshake_TamperedToken_Rejected() {
        String invalidToken = parentBToken + "tampered_signature";
        String wsUrl = "ws://localhost:" + port + "/chat?token=" + invalidToken;

        assertThrows(ExecutionException.class, () -> {
            new StandardWebSocketClient().doHandshake(
                new ChatWebSocketHandler(), 
                new WebSocketHttpHeaders(), 
                URI.create(wsUrl)
            ).get(2, TimeUnit.SECONDS);
        });
    }

    @Test
    @DisplayName("SEC-CHAT-002: Từ chối xử lý khi gửi tin nhắn vào hội thoại lạ")
    void testSendMessage_NotAMember_Rejected() throws Exception {
        // Stranger C connect WebSocket thành công bằng token hợp lệ của chính họ
        String wsUrl = "ws://localhost:" + port + "/chat?token=" + strangerCToken;
        CompletableFuture<WebSocketSession> sessionFuture = 
            new StandardWebSocketClient().doHandshake(new ChatWebSocketHandler(), new WebSocketHttpHeaders(), URI.create(wsUrl));
        WebSocketSession session = sessionFuture.get(2, TimeUnit.SECONDS);

        // Stranger C cố tình gửi tin nhắn vào cuộc hội thoại 123 (chỉ có Giáo viên A và Phụ huynh B)
        String exploitPayload = "{"
            + "\"type\":\"message.send\","
            + "\"conversationId\":123,"
            + "\"clientMessageId\":\"fe-exploit-999\","
            + "\"messageType\":\"TEXT\","
            + "\"content\":\"Hack message\""
            + "}";

        // Chuẩn bị bắt sự kiện trả về để kiểm tra phản hồi lỗi
        final CompletableFuture<String> responseFuture = new CompletableFuture<>();
        WebSocketSession testSession = new StandardWebSocketClient().doHandshake(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                responseFuture.complete(message.getPayload());
            }
        }, new WebSocketHttpHeaders(), URI.create(wsUrl)).get(2, TimeUnit.SECONDS);

        testSession.sendMessage(new TextMessage(exploitPayload));

        String errorResponse = responseFuture.get(3, TimeUnit.SECONDS);
        assertTrue(errorResponse.contains("NOT_CONVERSATION_MEMBER"));
        assertTrue(errorResponse.contains("Bạn không thuộc cuộc hội thoại này"));
        
        testSession.close();
    }

    @Test
    @DisplayName("SEC-CHAT-003: Chặn GET messages của cuộc hội thoại khác qua REST API")
    void testGetMessages_StrangerRetrieval_Forbidden() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(strangerCToken); // Gửi token của stranger C

        // Gọi GET /conversations/123 (đây là hội thoại của A và B)
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/conversations/123", 
            HttpMethod.GET, 
            new HttpEntity<>(headers), 
            String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode()); // 403 Forbidden
    }
}
```

---

## 3. Checklist Bảo mật cần thực hiện trước khi Release

- [ ] **Xác thực Handshake**: Token JWT phải được truyền qua URL query param `?token=` (do thư viện WebSocket phía client khó tùy chỉnh headers) và phải được kiểm tra tính hợp lệ trước khi nâng cấp kết nối HTTP -> WS.
- [ ] **Ràng buộc Quyền hạn (Membership Auth)**: Mọi thao tác qua WebSocket (`message.send`, `message.delivered`, `message.read`, `typing.start`, `typing.stop`) phải kiểm tra `userId` lấy ra từ WS Session Attributes có thuộc cuộc hội thoại `conversationId` trong DB hay không.
- [ ] **Mã hóa đường truyền**: Toàn bộ luồng kết nối trên production phải sử dụng giao thức bảo mật `wss://` (WebSocket Secure) và `https://` để chống nghe lén dữ liệu.
- [ ] **Chống ghi Log secret**: Đảm bảo các dữ liệu nhạy cảm (JWT Token, Mật khẩu, Thông tin PII của Học sinh) không được in trực tiếp lên các công cụ log hệ thống (Splunk, Kibana, Console).
- [ ] **Rate Limiting**: Giới hạn tần suất thiết lập kết nối WebSocket và tần suất gửi tin nhắn (ví dụ: tối đa 20 tin nhắn/phút cho mỗi kết nối) để phòng chống tấn công từ chối dịch vụ (DDoS / Socket Spamming).
- [ ] **Sanitization**: Tất cả nội dung tin nhắn (`content` dạng TEXT) phải được lọc bỏ các thẻ HTML/Script (XSS Prevention) trước khi lưu trữ vào DB hoặc đẩy đi qua các client khác.
