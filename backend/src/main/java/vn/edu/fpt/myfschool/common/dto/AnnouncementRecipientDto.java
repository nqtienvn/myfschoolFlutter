package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.UserRole;

import java.time.LocalDateTime;
import java.util.List;

public record AnnouncementRecipientDto(
    Long userId,
    String userName,
    UserRole role,
    List<String> studentNames,
    List<String> classNames,
    LocalDateTime readAt,
    LocalDateTime acknowledgedAt,
    String replyText,
    LocalDateTime repliedAt,
    String status
) {}
