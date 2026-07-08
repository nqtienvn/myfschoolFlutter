package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.controller.entity.Parent;
import vn.edu.fpt.myfschool.controller.entity.Student;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {
    Optional<Parent> findByUserId(Long userId);

    @Query("SELECT sg.student FROM StudentGuardian sg WHERE sg.guardian.id = :parentId")
    List<Student> findChildrenByParentId(@Param("parentId") Long parentId);
}
