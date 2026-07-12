package vn.edu.fpt.myfschool.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.NotificationDto;
import vn.edu.fpt.myfschool.entity.Notification;
import vn.edu.fpt.myfschool.repository.NotificationRepository;
import vn.edu.fpt.myfschool.websocket.WebSocketSessionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("notificationService")
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    @Override
    public List<NotificationDto> getNotifications(Long userId, String tag) {
        List<Notification> notifs;
        if (tag != null && !tag.isBlank()) {
            notifs = notificationRepository.findByUserIdAndTagOrderByCreatedAtDesc(userId, tag);
        } else {
            notifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return notifs.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.markAsRead(notificationId, userId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    public NotificationDto createNotification(Long userId, String title, String body, String tag) {
        return createNotification(userId, title, body, tag, null, null);
    }

    @Override
    public NotificationDto createNotification(
            Long userId, String title, String body, String tag,
            Long relatedId, String relatedType) {
        Notification n = new Notification();
        User user = new User();
        user.setId(userId);
        n.setUser(user);
        n.setTitle(title);
        n.setBody(body);
        n.setTag(tag);
        n.setRelatedId(relatedId);
        n.setRelatedType(relatedType);
        n.setIsRead(false);
        NotificationDto saved = toDto(notificationRepository.save(n));
        Runnable push = () -> pushRealtime(userId, saved);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push.run();
                }
            });
        } else {
            push.run();
        }
        return saved;
    }

    private void pushRealtime(Long userId, NotificationDto notification) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "notification.new");
        event.put("notification", notification);
        event.put("unreadCount", notificationRepository.countByUserIdAndIsReadFalse(userId));
        try {
            sessionManager.sendToUser(userId, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ignored) {
        }
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getTitle(), n.getBody(), n.getTag(),
            n.getIsRead(), n.getRelatedId() != null
                ? n.getRelatedId()
                : (n.getTuitionBill() != null ? n.getTuitionBill().getId() : null),
            n.getRelatedType() != null
                ? n.getRelatedType()
                : (n.getTuitionBill() != null ? "TUITION_BILL" : "SYSTEM"),
            n.getCreatedAt());
    }
}
