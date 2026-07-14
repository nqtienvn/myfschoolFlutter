package vn.edu.fpt.myfschool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.myfschool.common.dto.NotificationDto;
import vn.edu.fpt.myfschool.entity.Notification;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.NotificationRepository;
import vn.edu.fpt.myfschool.service.impl.NotificationServiceImpl;
import vn.edu.fpt.myfschool.websocket.WebSocketSessionManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock NotificationRepository notificationRepository;
    @Mock WebSocketSessionManager sessionManager;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(
                notificationRepository,
                sessionManager,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void getNotifications_filters_by_user_and_tag_and_preserves_related_metadata() {
        Notification notification = notification(7L, "Học phí", "TUITION", 41L, "TUITION_BILL");
        when(notificationRepository.findByUserIdAndTagOrderByCreatedAtDesc(7L, "TUITION"))
                .thenReturn(List.of(notification));

        List<NotificationDto> result = service.getNotifications(7L, "TUITION");

        assertEquals(1, result.size());
        NotificationDto dto = result.getFirst();
        assertAll(
                () -> assertEquals(15L, dto.id()),
                () -> assertEquals("Học phí", dto.title()),
                () -> assertEquals("TUITION", dto.tag()),
                () -> assertFalse(dto.isRead()),
                () -> assertEquals(41L, dto.relatedId()),
                () -> assertEquals("TUITION_BILL", dto.relatedType())
        );
        verify(notificationRepository).findByUserIdAndTagOrderByCreatedAtDesc(7L, "TUITION");
        verify(notificationRepository, never()).findByUserIdOrderByCreatedAtDesc(7L);
    }

    @Test
    void getNotifications_treats_a_blank_tag_as_no_filter() {
        Notification notification = notification(7L, "Lịch học", null, null, null);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(notification));

        NotificationDto dto = service.getNotifications(7L, "  ").getFirst();

        assertNull(dto.relatedId());
        assertEquals("SYSTEM", dto.relatedType());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(7L);
        verify(notificationRepository, never())
                .findByUserIdAndTagOrderByCreatedAtDesc(eq(7L), any());
    }

    @Test
    void createNotification_persists_the_owner_and_pushes_the_new_unread_count() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(22L);
            saved.setCreatedAt(LocalDateTime.of(2026, 7, 14, 10, 30));
            return saved;
        });
        when(notificationRepository.countByUserIdAndIsReadFalse(7L)).thenReturn(3L);

        NotificationDto result = service.createNotification(
                7L,
                "Thời khóa biểu mới",
                "Thời khóa biểu đã được phát hành.",
                "SCHEDULE",
                9L,
                "TIMETABLE"
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertAll(
                () -> assertEquals(7L, saved.getUser().getId()),
                () -> assertEquals("SCHEDULE", saved.getTag()),
                () -> assertEquals(9L, saved.getRelatedId()),
                () -> assertEquals("TIMETABLE", saved.getRelatedType()),
                () -> assertFalse(saved.getIsRead()),
                () -> assertEquals(22L, result.id())
        );
        verify(sessionManager).sendToUser(eq(7L), argThat(payload ->
                payload.contains("\"type\":\"notification.new\"")
                        && payload.contains("\"unreadCount\":3")
                        && payload.contains("\"id\":22")));
    }

    @Test
    void read_operations_delegate_with_the_authenticated_user_scope() {
        when(notificationRepository.countByUserIdAndIsReadFalse(7L)).thenReturn(2L);

        assertEquals(2L, service.getUnreadCount(7L));
        service.markAsRead(22L, 7L);
        service.markAllAsRead(7L);

        verify(notificationRepository).countByUserIdAndIsReadFalse(7L);
        verify(notificationRepository).markAsRead(22L, 7L);
        verify(notificationRepository).markAllAsRead(7L);
    }

    private Notification notification(Long userId, String title, String tag,
                                      Long relatedId, String relatedType) {
        User user = new User();
        user.setId(userId);
        Notification notification = new Notification();
        notification.setId(15L);
        notification.setUser(user);
        notification.setTitle(title);
        notification.setBody("Nội dung");
        notification.setTag(tag);
        notification.setRelatedId(relatedId);
        notification.setRelatedType(relatedType);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.of(2026, 7, 14, 9, 0));
        return notification;
    }
}
