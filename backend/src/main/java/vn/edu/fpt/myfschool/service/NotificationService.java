package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.NotificationDto;

import java.util.List;

public interface NotificationService {
    List<NotificationDto> getNotifications(Long userId, String tag);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    NotificationDto createNotification(Long userId, String title, String body, String tag);

    NotificationDto createNotification(
        Long userId, String title, String body, String tag,
        Long relatedId, String relatedType);
}
