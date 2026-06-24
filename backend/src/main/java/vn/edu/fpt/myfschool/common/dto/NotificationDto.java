package vn.edu.fpt.myfschool.common.dto;

import java.time.LocalDateTime;

public record NotificationDto(
    Long id, String title, String body, String tag, boolean isRead,
    Long relatedId, String relatedType, LocalDateTime createdAt
) {}
