package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.Announcement;
import vn.edu.fpt.myfschool.common.enums.TargetRole;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    @Query("SELECT DISTINCT a FROM Announcement a JOIN a.announcementClasses ac " +
           "WHERE ac.cls.id IN :classIds AND (a.targetRole = :targetRole OR a.targetRole = 'ALL') " +
           "ORDER BY a.createdAt DESC")
    List<Announcement> findByClassesAndTargetRole(@Param("classIds") List<Long> classIds,
                                                   @Param("targetRole") TargetRole targetRole);

    @Query("SELECT COUNT(a) FROM Announcement a JOIN a.announcementClasses ac " +
           "WHERE ac.cls.id IN :classIds AND (a.targetRole = :targetRole OR a.targetRole = 'ALL') " +
           "AND a.id NOT IN (SELECT ar.announcement.id FROM AnnouncementRead ar WHERE ar.user.id = :userId)")
    long countUnread(@Param("classIds") List<Long> classIds,
                      @Param("targetRole") TargetRole targetRole,
                      @Param("userId") Long userId);
}
