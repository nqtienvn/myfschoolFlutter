package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.AnnouncementPolicySetting;

import java.util.Optional;

public interface AnnouncementPolicySettingRepository extends JpaRepository<AnnouncementPolicySetting, Long> {
    Optional<AnnouncementPolicySetting> findByAcademicYearId(Long academicYearId);
}
