package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.myfschool.entity.AnnouncementPolicyViolation;

import java.util.List;
import java.util.Collection;

public interface AnnouncementPolicyViolationRepository extends JpaRepository<AnnouncementPolicyViolation, Long> {
    List<AnnouncementPolicyViolation> findByAnnouncementIdOrderById(Long announcementId);
    List<AnnouncementPolicyViolation> findByAnnouncementIdInOrderById(
            Collection<Long> announcementIds);

    @Query("SELECT v FROM AnnouncementPolicyViolation v " +
           "WHERE v.rule.academicYear.id = :academicYearId")
    List<AnnouncementPolicyViolation> findWithRuleByAcademicYearId(
            @Param("academicYearId") Long academicYearId);
}
