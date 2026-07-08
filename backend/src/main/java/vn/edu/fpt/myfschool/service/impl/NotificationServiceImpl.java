package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.controller.entity.User;
import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.NotificationDto;
import vn.edu.fpt.myfschool.controller.entity.Notification;
import vn.edu.fpt.myfschool.repository.NotificationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service("notificationService")
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

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
    public void createNotification(Long userId, String title, String body, String tag) {
        Notification n = new Notification();
        User user = new User();
        user.setId(userId);
        n.setUser(user);
        n.setTitle(title);
        n.setBody(body);
        n.setTag(tag);
        n.setIsRead(false);
        notificationRepository.save(n);
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getTitle(), n.getBody(), n.getTag(),
            n.getIsRead(), n.getTuitionBill() != null ? n.getTuitionBill().getId() : null,
            "SYSTEM", n.getCreatedAt());
    }
}
