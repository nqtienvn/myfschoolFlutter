package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.AnnouncementRead;
import java.util.Optional;

@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {
    Optional<AnnouncementRead> findByAnnouncementIdAndUserId(Long announcementId, Long userId);
    boolean existsByAnnouncementIdAndUserId(Long announcementId, Long userId);
    long countByAnnouncementIdAndReadAtIsNotNull(Long announcementId);
}
