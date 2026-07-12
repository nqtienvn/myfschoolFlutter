package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.TargetRole;
import vn.edu.fpt.myfschool.common.enums.UserRole;

import java.util.List;
import java.util.Map;

public interface AnnouncementService {
    AnnouncementDto createAnnouncement(String title, String body, TargetRole targetRole,
                                               boolean requiresReply, Long academicYearId, List<Long> classIds, Long teacherUserId);

    AnnouncementDto updateAnnouncement(Long id, String title, String body, TargetRole targetRole,
                                       Long academicYearId, List<Long> classIds, Long userId);
    void deleteAnnouncement(Long id, Long userId, UserRole role);
    List<AnnouncementDto> getAdminAnnouncements(Long academicYearId, String status);
    AnnouncementDto review(Long id, boolean approve, String reason, Long adminUserId);
    AnnouncementDto createAdminAnnouncement(String title, String body, Long academicYearId,
                                             String recipientScope, TargetRole targetRole,
                                             List<Long> classIds, String teacherAudience,
                                             Long subjectId, Long adminUserId);
    List<Map<String, Object>> getEligibleClasses(Long academicYearId, Long teacherUserId);

    List<AnnouncementDto> getMyAnnouncements(Long teacherUserId);

    AnnouncementDto getAnnouncementDetail(Long announcementId, Long userId, UserRole role);

    List<AnnouncementDto> getAnnouncements(Long userId, UserRole role);

    void markAsRead(Long announcementId, Long userId, UserRole role);

    long getUnreadCount(Long userId, UserRole role);
}
