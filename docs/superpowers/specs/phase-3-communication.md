# Phase 3 — Communication (Messaging + Announcements + Notifications + WebSocket)

> Tables: `conversations`, `conversation_participants`, `messages`, `announcements`, `announcement_classes`, `announcement_reads`, `notifications`
> Tổng: **7 tables**, **~45 API endpoints + WebSocket**

---

## 3A. Conversations + Messages (Messenger-style)

### 3A.1. Entities

```java
@Entity
@Table(name = "conversations")
public class Conversation extends BaseEntity {

    @Column(columnDefinition = "TEXT")
    private String lastMessage;  // Denormalized: tin nhắn cuối

    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConversationParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<Message> messages = new ArrayList<>();
}

@Entity
@Table(name = "conversation_participants",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"conversation_id", "user_id"}))
public class ConversationParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cp_conversation"))
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cp_user"))
    private User user;

    private LocalDateTime joinedAt;

    private LocalDateTime lastReadAt;  // → badge unread count
}

@Entity
@Table(name = "messages")
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_msg_conversation"))
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_msg_sender"))
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attachment> attachments = new ArrayList<>();
}
```

### 3A.2. DTOs

#### Request

```java
// --- Create or find existing conversation ---
// PARENT ↔ TEACHER (GV chủ nhiệm lớp của con)
// STUDENT ↔ TEACHER (GV chủ nhiệm)
public record CreateConversationRequest(
    @NotNull Long otherUserId   // Teacher or Parent
) {}

// --- Send message ---
public record SendMessageRequest(
    @NotBlank String content
) {}

// --- Mark as read ---
public record MarkReadRequest(
    @NotNull Long lastMessageId
) {}
```

#### Response

```java
// --- Conversation list item ---
public record ConversationDto(
    Long id,
    String lastMessage,
    LocalDateTime lastMessageAt,
    int unreadCount,
    ParticipantDto otherParticipant
) {}

public record ParticipantDto(
    Long userId,
    String name,
    String avatar,
    UserRole role
) {}

// --- Conversation detail with messages ---
public record ConversationDetailDto(
    Long id,
    ParticipantDto otherParticipant,
    List<MessageDto> messages
) {}

// --- Message ---
public record MessageDto(
    Long id,
    Long senderId,
    String senderName,
    String content,
    boolean isMine,      // computed: senderId == currentUserId
    LocalDateTime createdAt,
    List<AttachmentDto> attachments
) {}

// --- Paged messages (for infinite scroll) ---
public record MessagePageDto(
    List<MessageDto> messages,
    boolean hasMore,
    Long oldestMessageId   // cursor for next page
) {}
```

### 3A.3. Repositories

```java
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Find all conversations for a user, ordered by lastMessageAt DESC
    @Query("SELECT c FROM Conversation c " +
           "JOIN c.participants p " +
           "WHERE p.user.id = :userId " +
           "ORDER BY c.lastMessageAt DESC NULLS LAST")
    List<Conversation> findConversationsByUserId(@Param("userId") Long userId);

    // Find existing 1:1 conversation between two users
    @Query("SELECT c FROM Conversation c " +
           "JOIN c.participants p1 JOIN c.participants p2 " +
           "WHERE p1.user.id = :userId1 AND p2.user.id = :userId2 " +
           "AND SIZE(c.participants) = 2")
    Optional<Conversation> findConversationBetweenUsers(
        @Param("userId1") Long userId1, @Param("userId2") Long userId2);
}

@Repository
public interface ConversationParticipantRepository
    extends JpaRepository<ConversationParticipant, Long> {

    List<ConversationParticipant> findByConversationId(Long conversationId);

    Optional<ConversationParticipant> findByConversationIdAndUserId(
        Long conversationId, Long userId);

    // Find all conversations where user is participant
    @Query("SELECT cp.conversation.id FROM ConversationParticipant cp WHERE cp.user.id = :userId")
    List<Long> findConversationIdsByUserId(@Param("userId") Long userId);
}

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Load messages for a conversation (paginated, newest first)
    List<Message> findByConversationIdOrderByCreatedAtDesc(
        Long conversationId, Pageable pageable);

    // Load messages older than cursor (infinite scroll)
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :convId " +
           "AND m.id < :beforeMessageId ORDER BY m.createdAt DESC")
    List<Message> findMessagesBefore(@Param("convId") Long convId,
                                      @Param("beforeMessageId") Long beforeMessageId,
                                      Pageable pageable);

    // Count unread messages in a conversation for a user
    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.conversation.id = :convId " +
           "AND m.sender.id != :userId " +
           "AND m.createdAt > " +
           "(SELECT COALESCE(cp.lastReadAt, cp.joinedAt) " +
           "FROM ConversationParticipant cp " +
           "WHERE cp.conversation.id = :convId AND cp.user.id = :userId)")
    long countUnread(@Param("convId") Long convId, @Param("userId") Long userId);
}
```

### 3A.4. Service

```java
@Service
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final WebSocketSessionManager webSocketSessionManager;

    // --- Get conversation list ---
    // Returns all conversations for current user, with unread count
    public List<ConversationDto> getConversations(Long userId) {
        // 1. Find all conversations where user is participant
        // 2. For each: find other participant, count unread messages
        // 3. Return ordered by lastMessageAt DESC
    }

    // --- Create or find conversation ---
    // 1:1 conversation between two users
    // If already exists → return existing
    public ConversationDto createOrFindConversation(Long userId, Long otherUserId) {
        // 1. Check if conversation already exists between 2 users
        // 2. If exists → return existing
        // 3. If not → create Conversation + 2 ConversationParticipants
        // 4. Return ConversationDto
    }

    // --- Get conversation detail with messages ---
    // Paginated: load latest N messages, cursor-based
    public ConversationDetailDto getConversationDetail(Long conversationId,
                                                        Long userId,
                                                        Long beforeMessageId,
                                                        int limit) {
        // 1. Verify user is participant
        // 2. Load messages (paginated by beforeMessageId cursor)
        // 3. Mark as read (update lastReadAt)
        // 4. Return ConversationDetailDto
    }

    // --- Send message ---
    public MessageDto sendMessage(Long conversationId, Long senderId,
                                   SendMessageRequest request) {
        // 1. Verify sender is participant
        // 2. Create Message
        // 3. Update conversation.lastMessage + lastMessageAt
        // 4. Save
        // 5. Push via WebSocket to other participant (if connected)
        // 6. Return MessageDto
    }

    // --- Mark as read ---
    public void markAsRead(Long conversationId, Long userId, Long lastMessageId) {
        // 1. Find participant record
        // 2. Update lastReadAt = message.createdAt
        // 3. This reduces unread count
    }

    // --- Get total unread count (for badge) ---
    public int getTotalUnreadCount(Long userId) {
        // Sum unread across all conversations
    }
}
```

### 3A.5. Message Service (separate for clarity)

```java
@Service
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;

    // --- Get messages (paginated) ---
    public MessagePageDto getMessages(Long conversationId, Long userId,
                                       Long beforeMessageId, int limit) {
        // 1. Verify user is participant in conversation
        // 2. Load messages before cursor (or latest if no cursor)
        // 3. Map to MessageDto with isMine flag
        // 4. Determine hasMore
        // 5. Return MessagePageDto
    }
}
```

---

## 3B. WebSocket (Raw, tự implement)

### 3B.1. Architecture

```
Client ──ws://host/ws/chat?token=JWT──► WebSocketAuthInterceptor
                                              │
                                              ▼ (validate JWT, set userId)
                                         ChatWebSocketHandler
                                              │
                                              ├── On message → parse JSON → route
                                              ├── On connect → register session
                                              ├── On close → remove session
                                              │
                                              ▼
                                         WebSocketSessionManager
                                         (Map<userId, Session>)
```

### 3B.2. Config

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler, "/ws/chat")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins("*");  // Dev only; restrict in prod
    }
}
```

### 3B.3. Auth Interceptor

```java
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                    ServerHttpResponse response,
                                    WebSocketHandler wsHandler,
                                    Map<String, Object> attributes) {
        // 1. Extract token from query param: ?token=xxx
        // 2. Validate JWT
        // 3. Store userId + role in attributes
        // 4. Return true if valid, false if not
    }

    @Override
    public void afterHandshake(...) {}
}
```

### 3B.4. Session Manager

```java
@Component
public class WebSocketSessionManager {

    // userId → Set<WebSocketSession> (user có thể login nhiều device)
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void addSession(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void removeSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(session);
            if (userSessions.isEmpty()) sessions.remove(userId);
        }
    }

    public void sendToUser(Long userId, String message) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.forEach(session -> {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            });
        }
    }

    public boolean isOnline(Long userId) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        return userSessions != null && !userSessions.isEmpty();
    }

    public Set<Long> getOnlineUserIds() {
        return sessions.keySet();
    }
}
```

### 3B.5. Handler

```java
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        sessionManager.addSession(userId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        sessionManager.removeSession(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Parse JSON message
        // {
        //   "type": "CHAT" | "MARK_READ" | "TYPING",
        //   "conversationId": 1,
        //   "content": "Hello",        // for CHAT
        //   "lastMessageId": 123       // for MARK_READ
        // }
        //
        // Handle:
        // CHAT → save to DB, push to other participant
        // MARK_READ → update lastReadAt
        // TYPING → push to other participant (ephemeral, not saved)
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = (Long) session.getAttributes().get("userId");
        sessionManager.removeSession(userId, session);
    }
}
```

### 3B.6. WebSocket Message Format

```json
// Client → Server
{
  "type": "CHAT",
  "conversationId": 1,
  "content": "Xin chào thầy!"
}

// Client → Server (mark read)
{
  "type": "MARK_READ",
  "conversationId": 1,
  "lastMessageId": 123
}

// Client → Server (typing indicator)
{
  "type": "TYPING",
  "conversationId": 1
}

// Server → Client (new message)
{
  "type": "NEW_MESSAGE",
  "conversationId": 1,
  "message": {
    "id": 124,
    "senderId": 5,
    "senderName": "Nguyễn Minh An",
    "content": "Xin chào thầy!",
    "createdAt": "2026-06-24T10:30:00"
  }
}

// Server → Client (read receipt)
{
  "type": "READ_RECEIPT",
  "conversationId": 1,
  "userId": 7,
  "lastReadAt": "2026-06-24T10:31:00"
}

// Server → Client (typing)
{
  "type": "TYPING",
  "conversationId": 1,
  "userId": 7,
  "userName": "Cô Hà"
}
```

---

## 3C. Announcements

### 3C.1. Entities

```java
@Entity
@Table(name = "announcements")
public class Announcement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ann_teacher"))
    private Teacher teacher;  // Người tạo

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TargetRole targetRole;  // PARENT | STUDENT | ALL

    @Column(nullable = false)
    private Boolean requiresReply = false;

    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AnnouncementClass> announcementClasses = new ArrayList<>();

    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AnnouncementRead> reads = new ArrayList<>();
}

@Entity
@Table(name = "announcement_classes",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"announcement_id", "class_id"}))
public class AnnouncementClass extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ac_announcement"))
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ac_class"))
    private Class cls;
}

@Entity
@Table(name = "announcement_reads",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"announcement_id", "user_id"}))
public class AnnouncementRead extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ar_announcement"))
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ar_user"))
    private User user;

    private LocalDateTime readAt;
}
```

### 3C.2. DTOs

```java
// --- Create announcement ---
public record CreateAnnouncementRequest(
    @NotBlank @Size(max = 500) String title,
    @NotBlank String body,
    @NotNull TargetRole targetRole,
    Boolean requiresReply,
    @NotEmpty List<Long> classIds   // Gửi tới lớp nào
) {}

// --- Announcement list item ---
public record AnnouncementDto(
    Long id,
    String title,
    String body,
    TargetRole targetRole,
    Boolean requiresReply,
    Long teacherId,
    String teacherName,
    List<String> classNames,     // Lớp nhận thông báo
    boolean isRead,              // Đã đọc chưa (computed per user)
    int totalRecipients,         // Tổng số người nhận
    int readCount,               // Số người đã đọc
    LocalDateTime createdAt
) {}

// --- Announcement detail ---
public record AnnouncementDetailDto(
    Long id,
    String title,
    String body,
    TargetRole targetRole,
    Boolean requiresReply,
    Long teacherId,
    String teacherName,
    List<String> classNames,
    boolean isRead,
    List<AnnouncementReadEntryDto> readEntries,  // Danh sách đã/chưa đọc
    LocalDateTime createdAt
) {}

public record AnnouncementReadEntryDto(
    Long userId,
    String userName,
    UserRole role,
    boolean isRead,
    LocalDateTime readAt
) {}
```

### 3C.3. Repository

```java
@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // GV xem thông báo đã gửi
    List<Announcement> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    // PH/HS xem thông báo cho lớp mình
    @Query("SELECT DISTINCT a FROM Announcement a " +
           "JOIN a.announcementClasses ac " +
           "WHERE ac.cls.id IN :classIds " +
           "AND (a.targetRole = :targetRole OR a.targetRole = 'ALL') " +
           "ORDER BY a.createdAt DESC")
    List<Announcement> findByClassesAndTargetRole(
        @Param("classIds") List<Long> classIds,
        @Param("targetRole") TargetRole targetRole);

    // Count unread announcements for user
    @Query("SELECT COUNT(a) FROM Announcement a " +
           "JOIN a.announcementClasses ac " +
           "WHERE ac.cls.id IN :classIds " +
           "AND (a.targetRole = :targetRole OR a.targetRole = 'ALL') " +
           "AND a.id NOT IN " +
           "(SELECT ar.announcement.id FROM AnnouncementRead ar WHERE ar.user.id = :userId)")
    long countUnread(@Param("classIds") List<Long> classIds,
                      @Param("targetRole") TargetRole targetRole,
                      @Param("userId") Long userId);
}

@Repository
public interface AnnouncementClassRepository extends JpaRepository<AnnouncementClass, Long> {
    List<AnnouncementClass> findByAnnouncementId(Long announcementId);
    void deleteByAnnouncementId(Long announcementId);
}

@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {

    Optional<AnnouncementRead> findByAnnouncementIdAndUserId(Long announcementId, Long userId);

    boolean existsByAnnouncementIdAndUserId(Long announcementId, Long userId);

    // Count reads for an announcement
    long countByAnnouncementIdAndReadAtIsNotNull(Long announcementId);

    // Find read entries for announcement
    @Query("SELECT ar FROM AnnouncementRead ar WHERE ar.announcement.id = :announcementId")
    List<AnnouncementRead> findByAnnouncement(@Param("announcementId") Long announcementId);
}
```

### 3C.4. Service

```java
@Service
@Transactional
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementClassRepository announcementClassRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final NotificationService notificationService;
    private final WebSocketSessionManager webSocketSessionManager;

    // ============================
    // TEACHER FUNCTIONS
    // ============================

    // --- Create announcement ---
    // GV tạo thông báo → gửi nhiều lớp
    public AnnouncementDto createAnnouncement(CreateAnnouncementRequest request,
                                               Long teacherUserId) {
        // 1. Verify teacher exists
        // 2. Create Announcement
        // 3. Create AnnouncementClass records for each classId
        // 4. Create AnnouncementRead records (one per target user) - lazily or eagerly?
        //    → Lazily: create when user first loads, track via query
        // 5. Send notification to affected users
        // 6. Return AnnouncementDto
    }

    // --- Get my sent announcements ---
    public List<AnnouncementDto> getMyAnnouncements(Long teacherUserId) {
        // 1. Find teacher by userId
        // 2. Load all announcements created by this teacher
        // 3. Include read stats (readCount, totalRecipients)
        // 4. Return list
    }

    // --- Get announcement detail with read tracking ---
    public AnnouncementDetailDto getAnnouncementDetail(Long announcementId,
                                                        Long teacherUserId) {
        // 1. Verify teacher owns this announcement
        // 2. Load announcement + classes + read entries
        // 3. Return AnnouncementDetailDto with full read tracking
    }

    // ============================
    // PARENT/STUDENT FUNCTIONS
    // ============================

    // --- Get announcements for my classes ---
    public List<AnnouncementDto> getAnnouncements(Long userId, UserRole role) {
        // 1. Find user's classes (student→currentClass, parent→children's classes)
        // 2. Filter by targetRole (match user's role)
        // 3. For each: check isRead (exists in announcement_reads?)
        // 4. Return list ordered by createdAt DESC
    }

    // --- Mark announcement as read ---
    public void markAsRead(Long announcementId, Long userId) {
        // 1. Check if already read (idempotent)
        // 2. If not: create AnnouncementRead with readAt = now
        // 3. If requiresReply: clear red badge
    }

    // --- Get unread count (for badge) ---
    public long getUnreadCount(Long userId, UserRole role) {
        // 1. Find user's classes
        // 2. Count announcements not in announcement_reads
    }

    // ============================
    // INTERNAL
    // ============================

    // --- Batch create read records ---
    // When announcement is created, pre-create read records for all target users
    private void createReadRecords(Announcement announcement, List<Long> classIds) {
        // 1. Find all users in target classes with matching role
        // 2. Create AnnouncementRead(user, announcement) for each
        //    readAt = null (unread)
    }
}
```

---

## 3D. Notifications (System Notifications)

### 3D.1. Entity

```java
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_noti_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tuition_bill_id",
                foreignKey = @ForeignKey(name = "fk_noti_bill"))
    private TuitionBill tuitionBill;  // optional

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(length = 50)
    private String tag;  // Học phí, CLB, Hệ thống, Đơn nghỉ...

    @Column(nullable = false)
    private Boolean isRead = false;
}
```

### 3D.2. DTOs

```java
// --- Notification list item ---
public record NotificationDto(
    Long id,
    String title,
    String body,
    String tag,
    boolean isRead,
    Long relatedId,       // ID of related entity (bill, leave request, etc.)
    String relatedType,   // "TUITION_BILL", "LEAVE_REQUEST", "ANNOUNCEMENT", etc.
    LocalDateTime createdAt
) {}
```

### 3D.3. Repository

```java
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // User's notifications, ordered by newest
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Unread count for badge
    long countByUserIdAndIsReadFalse(Long userId);

    // Mark all as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);

    // Mark single as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.user.id = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    // Find by tag
    List<Notification> findByUserIdAndTagOrderByCreatedAtDesc(Long userId, String tag);
}
```

### 3D.4. Service

```java
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WebSocketSessionManager webSocketSessionManager;
    private final ObjectMapper objectMapper;

    // --- Get notifications ---
    public List<NotificationDto> getNotifications(Long userId, String tag) {
        // 1. Load notifications (optionally filtered by tag)
        // 2. Return list
    }

    // --- Get unread count ---
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // --- Mark single as read ---
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.markAsRead(notificationId, userId);
    }

    // --- Mark all as read ---
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    // --- Create notification (internal, called by other services) ---
    public void createNotification(Long userId, String title, String body,
                                    String tag, Long relatedId, String relatedType) {
        // 1. Create Notification entity
        // 2. Save
        // 3. Push via WebSocket if user online
        //    { "type": "NOTIFICATION", "notification": {...}, "unreadCount": N }
    }

    // --- Create bulk notifications ---
    public void createBulkNotifications(List<Long> userIds, String title,
                                         String body, String tag) {
        // For each userId: createNotification
    }
}
```

---

## 3E. Controllers

```java
@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations", description = "Hộp thoại tin nhắn")
@SecurityRequirement(name = "Bearer Authentication")
public class ConversationController {

    private final ConversationService conversationService;

    // GET /api/conversations
    // Any role: list my conversations
    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<ConversationDto>>> getConversations() { ... }

    // POST /api/conversations
    // Create or find existing conversation with another user
    @PostMapping
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<ConversationDto>> createConversation(
        @Valid @RequestBody CreateConversationRequest request) { ... }

    // GET /api/conversations/{id}?beforeMessageId=100&limit=20
    // Get conversation detail with paginated messages
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<ConversationDetailDto>> getConversationDetail(
        @PathVariable Long id,
        @RequestParam(required = false) Long beforeMessageId,
        @RequestParam(defaultValue = "20") int limit) { ... }

    // POST /api/conversations/{id}/messages
    // Send message (also via WebSocket, HTTP fallback)
    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
        @PathVariable Long id,
        @Valid @RequestBody SendMessageRequest request) { ... }

    // PUT /api/conversations/{id}/read
    // Mark conversation as read
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
        @PathVariable Long id,
        @Valid @RequestBody MarkReadRequest request) { ... }

    // GET /api/conversations/unread-count
    // Total unread messages count
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount() { ... }
}

@RestController
@RequestMapping("/api/announcements")
@Tag(name = "Announcements", description = "Thông báo lớp học")
@SecurityRequirement(name = "Bearer Authentication")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    // POST /api/announcements
    // TEACHER: create and send announcement
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AnnouncementDto>> createAnnouncement(
        @Valid @RequestBody CreateAnnouncementRequest request) { ... }

    // GET /api/announcements/mine
    // TEACHER: announcements I sent
    @GetMapping("/mine")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<AnnouncementDto>>> getMyAnnouncements() { ... }

    // GET /api/announcements/{id}
    // TEACHER: announcement detail with read tracking
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<AnnouncementDetailDto>> getAnnouncementDetail(
        @PathVariable Long id) { ... }

    // GET /api/announcements
    // PARENT/STUDENT: announcements for my classes
    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<AnnouncementDto>>> getAnnouncements() { ... }

    // PUT /api/announcements/{id}/read
    // PARENT/STUDENT: mark as read
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) { ... }

    // GET /api/announcements/unread-count
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() { ... }
}

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Thông báo hệ thống")
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationController {

    private final NotificationService notificationService;

    // GET /api/notifications?tag=Học phí
    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getNotifications(
        @RequestParam(required = false) String tag) { ... }

    // GET /api/notifications/unread-count
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() { ... }

    // PUT /api/notifications/{id}/read
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) { ... }

    // PUT /api/notifications/read-all
    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() { ... }
}
```

---

## Phase 3 — Summary

### API Endpoints

| # | Method | Endpoint | Auth | Description |
|---|--------|----------|------|-------------|
| 1 | GET | `/api/conversations` | Any | Danh sách hộp thoại |
| 2 | POST | `/api/conversations` | Any | Tạo/tìm hộp thoại |
| 3 | GET | `/api/conversations/{id}` | Any | Chi tiết + tin nhắn |
| 4 | POST | `/api/conversations/{id}/messages` | Any | Gửi tin nhắn (HTTP) |
| 5 | PUT | `/api/conversations/{id}/read` | Any | Đánh dấu đã đọc |
| 6 | GET | `/api/conversations/unread-count` | Any | Tổng tin chưa đọc |
| 7 | POST | `/api/announcements` | TEACHER | Tạo thông báo |
| 8 | GET | `/api/announcements/mine` | TEACHER | Thông báo đã gửi |
| 9 | GET | `/api/announcements/{id}` | Any | Chi tiết + tracking |
| 10 | GET | `/api/announcements` | PARENT/STUDENT | Thông báo cho tôi |
| 11 | PUT | `/api/announcements/{id}/read` | PARENT/STUDENT | Đánh dấu đã đọc |
| 12 | GET | `/api/announcements/unread-count` | PARENT/STUDENT | Số chưa đọc |
| 13 | GET | `/api/notifications` | Any | Thông báo hệ thống |
| 14 | GET | `/api/notifications/unread-count` | Any | Số chưa đọc |
| 15 | PUT | `/api/notifications/{id}/read` | Any | Đọc 1 thông báo |
| 16 | PUT | `/api/notifications/read-all` | Any | Đọc tất cả |

### WebSocket Endpoints

| Path | Description |
|------|-------------|
| `ws://host/ws/chat?token=JWT` | Real-time messaging |
| Message types: `CHAT`, `MARK_READ`, `TYPING`, `NEW_MESSAGE`, `READ_RECEIPT`, `NOTIFICATION` |

---

*Tiếp tục: [Phase 4 — Business](phase-4-business.md)*
