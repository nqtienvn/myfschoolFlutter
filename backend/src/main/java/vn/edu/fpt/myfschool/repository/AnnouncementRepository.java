package vn.edu.fpt.myfschool.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.common.enums.AnnouncementDeliveryStatus;
import vn.edu.fpt.myfschool.common.enums.TargetRole;
import vn.edu.fpt.myfschool.entity.Announcement;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);
    long countByAcademicYearId(Long academicYearId);
    long countByAcademicYearIdAndDeliveryStatus(Long academicYearId, AnnouncementDeliveryStatus deliveryStatus);

    @EntityGraph(attributePaths = {"sender", "teacher", "academicYear", "retryOfAnnouncement"})
    @Query("SELECT a FROM Announcement a WHERE a.academicYear.id = :academicYearId " +
           "AND (:status IS NULL OR a.deliveryStatus = :status) " +
           "AND (:keyword IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.body) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.sender.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) ")
    Page<Announcement> searchAdminAnnouncements(
            @Param("academicYearId") Long academicYearId,
            @Param("status") AnnouncementDeliveryStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT DISTINCT a FROM Announcement a JOIN a.announcementClasses ac " +
           "WHERE ac.cls.id IN :classIds AND a.targetRole IN :targetRoles " +
           "AND a.deliveryStatus = vn.edu.fpt.myfschool.common.enums.AnnouncementDeliveryStatus.PUBLISHED " +
           "ORDER BY a.createdAt DESC")
    List<Announcement> findByClassesAndTargetRoles(@Param("classIds") List<Long> classIds,
                                                    @Param("targetRoles") List<TargetRole> targetRoles);

    @Query("SELECT COUNT(DISTINCT a) FROM Announcement a JOIN a.announcementClasses ac " +
           "WHERE ac.cls.id IN :classIds AND a.targetRole IN :targetRoles " +
           "AND a.deliveryStatus = vn.edu.fpt.myfschool.common.enums.AnnouncementDeliveryStatus.PUBLISHED " +
           "AND a.id NOT IN (SELECT ar.announcement.id FROM AnnouncementRead ar WHERE ar.user.id = :userId)")
    long countUnread(@Param("classIds") List<Long> classIds,
                     @Param("targetRoles") List<TargetRole> targetRoles,
                     @Param("userId") Long userId);
}
