package vn.edu.fpt.myfschool.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.myfschool.common.dto.AnnouncementDto;
import vn.edu.fpt.myfschool.common.dto.AnnouncementPolicyDto;
import vn.edu.fpt.myfschool.common.dto.AnnouncementPolicyUpdateRequest;
import vn.edu.fpt.myfschool.common.dto.AnnouncementRecipientDto;
import vn.edu.fpt.myfschool.common.dto.AnnouncementSubmissionResultDto;
import vn.edu.fpt.myfschool.common.dto.AnnouncementSummaryDto;
import vn.edu.fpt.myfschool.common.enums.AnnouncementDeliveryStatus;
import vn.edu.fpt.myfschool.common.enums.TargetRole;
import vn.edu.fpt.myfschool.common.enums.UserRole;

import java.util.List;
import java.util.Map;

public interface AnnouncementService {
    AnnouncementSubmissionResultDto createAnnouncement(String title, String body, TargetRole targetRole,
            Long academicYearId, List<Long> classIds, Long retryOfAnnouncementId, Long teacherUserId);

    void deleteAnnouncement(Long id, Long userId, UserRole role);

    Page<AnnouncementDto> getAdminAnnouncements(Long academicYearId, AnnouncementDeliveryStatus status,
            String keyword, int page, int size);

    AnnouncementSummaryDto getAdminSummary(Long academicYearId);

    AnnouncementPolicyDto getPolicy(Long academicYearId);

    AnnouncementPolicyDto updatePolicy(AnnouncementPolicyUpdateRequest request, Long adminUserId);

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
