package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.TargetRole;
import vn.edu.fpt.myfschool.common.enums.UserRole;

import java.util.List;

public interface AnnouncementService {
    AnnouncementDto createAnnouncement(String title, String body, TargetRole targetRole,
                                               boolean requiresReply, List<Long> classIds, Long teacherUserId);

    List<AnnouncementDto> getMyAnnouncements(Long teacherUserId);

    AnnouncementDto getAnnouncementDetail(Long announcementId, Long userId, UserRole role);

    List<AnnouncementDto> getAnnouncements(Long userId, UserRole role);

    void markAsRead(Long announcementId, Long userId, UserRole role);

    long getUnreadCount(Long userId, UserRole role);
}
