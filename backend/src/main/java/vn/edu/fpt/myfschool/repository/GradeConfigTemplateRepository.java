package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.GradeConfigTemplate;
import java.util.List;

public interface GradeConfigTemplateRepository extends JpaRepository<GradeConfigTemplate,Long> {
    List<GradeConfigTemplate> findByActiveTrueOrderByNameAscVersionDesc();
}
