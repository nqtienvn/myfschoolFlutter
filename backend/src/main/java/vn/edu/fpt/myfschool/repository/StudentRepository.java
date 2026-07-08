package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.controller.entity.Student;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUserId(Long userId);
    Optional<Student> findByStudentCode(String studentCode);
    List<Student> findByCurrentClassId(Long classId);
    boolean existsByStudentCode(String studentCode);

    @Query("SELECT s FROM Student s WHERE s.currentClass.id = :classId AND " +
           "(LOWER(s.user.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "s.studentCode LIKE CONCAT('%', :keyword, '%'))")
    List<Student> searchByClassAndKeyword(@Param("classId") Long classId,
                                           @Param("keyword") String keyword);
}
