package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.AnnouncementRead;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {
    Optional<AnnouncementRead> findByAnnouncementIdAndUserId(Long announcementId, Long userId);
    boolean existsByAnnouncementIdAndUserId(Long announcementId, Long userId);
    long countByAnnouncementId(Long announcementId);
    long countByAnnouncementIdAndReadAtIsNotNull(Long announcementId);
    long countByUserIdAndReadAtIsNull(Long userId);
    List<AnnouncementRead> findByAnnouncementId(Long announcementId);
    List<AnnouncementRead> findByUserIdOrderByAnnouncementCreatedAtDesc(Long userId);
    long countByAnnouncementIdInAndUserIdInAndReadAtIsNotNull(
        Collection<Long> announcementIds, Collection<Long> userIds);
}
