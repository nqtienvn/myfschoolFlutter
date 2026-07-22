package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.GradeItem;
import java.util.Collection;
import java.util.List;

@Repository
public interface GradeItemRepository extends JpaRepository<GradeItem, Long> {
    List<GradeItem> findByGradeBookIdOrderByOrderAsc(Long gradeBookId);
    List<GradeItem> findByGradeBookIdIn(Collection<Long> gradeBookIds);
}
