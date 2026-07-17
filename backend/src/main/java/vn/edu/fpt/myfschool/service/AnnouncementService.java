package vn.edu.fpt.myfschool.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.myfschool.common.dto.AnnouncementDto;
import vn.edu.fpt.myfschool.common.dto.AnnouncementRecipientDto;
import vn.edu.fpt.myfschool.common.enums.TargetRole;
import vn.edu.fpt.myfschool.common.enums.UserRole;

import java.util.List;
import java.util.Map;

public interface AnnouncementService {
    AnnouncementDto createAnnouncement(String title, String body, TargetRole targetRole,
            Long academicYearId, List<Long> classIds, Long teacherUserId);

    AnnouncementDto updateAnnouncement(Long id, String title, String body, TargetRole targetRole,
            Long academicYearId, List<Long> classIds, Long userId);

    void deleteAnnouncement(Long id, Long userId, UserRole role);

    List<AnnouncementDto> getAdminAnnouncements(Long academicYearId, String status);

    AnnouncementDto review(Long id, boolean approve, String reason, Long adminUserId);

    AnnouncementDto createAdminAnnouncement(String title, String body, Long academicYearId,
            Long adminUserId);

    List<Map<String, Object>> getEligibleClasses(Long academicYearId, Long teacherUserId);

    List<AnnouncementDto> getMyAnnouncements(Long teacherUserId, Long academicYearId);

    AnnouncementDto getAnnouncementDetail(Long announcementId, Long userId, UserRole role);

    List<AnnouncementDto> getAnnouncements(Long userId, UserRole role, Long academicYearId);

    void markAsRead(Long announcementId, Long userId, UserRole role);

    Page<AnnouncementRecipientDto> getRecipients(Long announcementId, Long academicYearId,
            Long classId, UserRole role, String status, String keyword, int page, int size,
            Long requesterId, UserRole requesterRole);

    long getUnreadCount(Long userId, UserRole role, Long academicYearId);
}
