package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.controller.entity.Teacher;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByUserId(Long userId);
    Optional<Teacher> findByEmployeeCode(String employeeCode);
    boolean existsByEmployeeCode(String employeeCode);
}
