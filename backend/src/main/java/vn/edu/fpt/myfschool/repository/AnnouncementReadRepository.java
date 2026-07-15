package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.AnnouncementRead;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {
    Optional<AnnouncementRead> findByAnnouncementIdAndUserId(Long announcementId, Long userId);
    boolean existsByAnnouncementIdAndUserId(Long announcementId, Long userId);
    long countByAnnouncementId(Long announcementId);
    long countByAnnouncementIdAndReadAtIsNotNull(Long announcementId);
    long countByAnnouncementIdAndAcknowledgedAtIsNotNull(Long announcementId);
    long countByAnnouncementIdAndRepliedAtIsNotNull(Long announcementId);
    long countByUserIdAndReadAtIsNull(Long userId);
    List<AnnouncementRead> findByAnnouncementId(Long announcementId);
    List<AnnouncementRead> findByUserIdOrderByAnnouncementCreatedAtDesc(Long userId);
    @Query("SELECT COUNT(ar) FROM AnnouncementRead ar " +
           "WHERE ar.user.id = :userId AND ar.announcement.requiresReply = true " +
           "AND ar.announcement.approvalStatus = 'APPROVED' " +
           "AND ar.acknowledgedAt IS NULL AND ar.repliedAt IS NULL")
    long countPendingActionByUserId(@Param("userId") Long userId);
    long countByAnnouncementIdInAndUserIdInAndReadAtIsNotNull(
        Collection<Long> announcementIds, Collection<Long> userIds);
}
