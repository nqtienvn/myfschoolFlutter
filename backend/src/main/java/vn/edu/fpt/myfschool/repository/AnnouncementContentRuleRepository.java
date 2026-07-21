package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.AnnouncementContentRule;

import java.util.List;

public interface AnnouncementContentRuleRepository extends JpaRepository<AnnouncementContentRule, Long> {
    List<AnnouncementContentRule> findByAcademicYearIdOrderById(Long academicYearId);
    void deleteByAcademicYearId(Long academicYearId);
}
