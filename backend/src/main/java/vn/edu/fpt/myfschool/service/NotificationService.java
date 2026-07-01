package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.NotificationDto;
import vn.edu.fpt.myfschool.entity.Notification;
import vn.edu.fpt.myfschool.repository.NotificationRepository;
import java.util.List;
import java.util.stream.Collectors;

public interface NotificationService {
    List<NotificationDto> getNotifications(Long userId, String tag);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    void createNotification(Long userId, String title, String body, String tag);
}
