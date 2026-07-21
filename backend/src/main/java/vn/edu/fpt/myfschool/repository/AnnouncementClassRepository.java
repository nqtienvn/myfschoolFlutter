package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.AnnouncementClass;
import java.util.Collection;
import java.util.List;

@Repository
public interface AnnouncementClassRepository extends JpaRepository<AnnouncementClass, Long> {
    List<AnnouncementClass> findByAnnouncementId(Long announcementId);
    List<AnnouncementClass> findByAnnouncementIdIn(Collection<Long> announcementIds);
}
