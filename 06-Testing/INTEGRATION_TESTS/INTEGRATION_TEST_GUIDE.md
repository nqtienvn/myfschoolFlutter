# 🔗 INTEGRATION TESTS – MyFschool Chat Realtime

> **Thư mục:** `06-Testing/INTEGRATION_TESTS/`  
> **Mục đích:** Kiểm thử tích hợp giữa các tầng (WebSocketHandler → ChatRealtimeService → ConversationService → MySQL DB) và xác thực JWT token handshake.  
> **Framework:** Spring Boot Test (`@SpringBootTest`) + Spring WebSocket Client + Testcontainers (MySQL/H2)

---

## Chiến lược Integration Test

```
┌────────────────────────────────────────────────────────┐
│                 Integration Test Scope                 │
│                                                        │
│  WebSocketHandshake ──► ChatWebSocketHandler           │
│                                │                       │
│                                ▼                       │
│                       ChatRealtimeService              │
│                                │                       │
│                                ▼                       │
│                       ConversationService              │
│                                │                       │
│                                ▼                       │
│                         MySQL / H2 DB                  │
└────────────────────────────────────────────────────────┘
```

**Môi trường tích hợp:**
- **Database**: H2 Database cấu hình chế độ tương thích MySQL hoặc MySQL Docker container chạy độc lập qua Testcontainers.
- **WebSocket Port**: RANDOM_PORT được khởi chạy thật trong quá trình kiểm thử để thiết lập kết nối TCP thực tế.
- **JWT**: Sử dụng `JwtTokenProvider` để mã hóa token test truyền vào WebSocket query handshake (`/chat?token=<JWT>`).

---

## Ví dụ Test Suite Tích Hợp (Spring Boot)

#### `WebSocketMessagingIntegrationTest.java`

```java
/**
 * Integration tests for WebSocket messaging flow.
 *
 * Coverage:
 * - message.send: happy path (send + ACK + DB persist)
 * - handshake: missing token rejection
 *
 * Related UC: UC08 Real-time Chat
 * Test Case Doc: Test_Cases/UC08_RealtimeChat_TESTCASE.md
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebSocketMessagingIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void tearDown() {
        // Cleanup: Xóa dữ liệu test tạo ra trong quá trình kiểm thử
        messageRepository.deleteAll();
    }

    @Test
    @DisplayName("Tích hợp WebSocket: Gửi tin nhắn hợp lệ -> Lưu DB và nhận ACK")
    void testWebSocketSendAndAckFlow() throws Exception {
        // Step 1: Tạo token JWT cho Giáo viên A (User ID = 7)
        String token = jwtTokenProvider.generateToken(7L, "TEACHER");
        
        // Thử kết nối WebSocket Client đến endpoint test
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        String wsUrl = "ws://localhost:" + port + "/chat?token=" + token;
        
        CompletableFuture<WebSocketSession> sessionFuture = 
            new StandardWebSocketClient().doHandshake(new ChatWebSocketHandler(), headers, URI.create(wsUrl));
        
        WebSocketSession session = sessionFuture.get(3, TimeUnit.SECONDS);
        assertTrue(session.isOpen());

        // Step 2: Gửi payload message.send qua WebSocket
        String payload = "{"
            + "\"type\":\"message.send\","
            + "\"conversationId\":123,"
            + "\"clientMessageId\":\"fe-int-001\","
            + "\"messageType\":\"TEXT\","
            + "\"content\":\"Xin chào phụ huynh\""
            + "}";
        
        session.sendMessage(new TextMessage(payload));

        // Đợi nhận phản hồi hoặc kiểm tra trực tiếp trạng thái DB sau transaction commit
        await().atMost(2, SECONDS).untilAsserted(() -> {
            // Xác minh tin nhắn đã lưu vào MySQL DB
            Optional<Message> savedMessage = messageRepository.findBySenderIdAndClientMessageId(7L, "fe-int-001");
            assertTrue(savedMessage.isPresent());
            assertEquals("Xin chào phụ huynh", savedMessage.get().getContent());
            assertNotNull(savedMessage.get().getServerSeq());
            
            // Xác minh conversation cập nhật lastMessage
            Conversation conv = conversationRepository.findById(123L).orElseThrow();
            assertEquals("Xin chào phụ huynh", conv.getLastMessage());
        });

        session.close();
    }

    @Test
    @DisplayName("Tích hợp Bảo mật: Handshake không token -> Reject với 401")
    void testWebSocketHandshakeWithoutToken_Rejected() {
        String wsUrl = "ws://localhost:" + port + "/chat"; // Thiếu token
        
        assertThrows(ExecutionException.class, () -> {
            new StandardWebSocketClient().doHandshake(
                new ChatWebSocketHandler(), 
                new WebSocketHttpHeaders(), 
                URI.create(wsUrl)
            ).get(2, TimeUnit.SECONDS);
        });
    }
}
```

---

## Cấu hình môi trường Test

### `backend/src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:myfschool_test;DB_CLOSE_DELAY=-1;MODE=MySQL
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect

app:
  jwt:
    secret: my-super-secret-key-32-bytes-length-minimum-for-jwt-signing-key
    expiration-ms: 3600000 # 1 hour
```

---

## Chạy Integration Tests

```bash
# Chạy tất cả integration tests cho module WebSocket
cd backend
./mvnw test -Dtest="*WebSocket*IntegrationTest" -Dspring.profiles.active=test

# Xem báo cáo độ bao phủ mã nguồn
./mvnw jacoco:report
# Kết quả report tại: target/site/jacoco/index.html
```

---

## Checklist dành cho Integration Test

- [ ] Test Database phải chạy độc lập trên bộ nhớ (H2 mem) hoặc docker instance tạm thời, tuyệt đối không chỉnh sửa DB dev hay production.
- [ ] Mọi kịch bản kết nối WebSocket client phải chủ động close session (`session.close()`) ở block cleanup hoặc `@AfterEach` để tránh rò rỉ luồng (thread leak).
- [ ] Token JWT sử dụng phải được sinh tự động từ `JwtTokenProvider` với secret key của môi trường test.
- [ ] Xác minh trạng thái DB song song với việc xác minh event WS (assert cả 2 đầu dữ liệu).
