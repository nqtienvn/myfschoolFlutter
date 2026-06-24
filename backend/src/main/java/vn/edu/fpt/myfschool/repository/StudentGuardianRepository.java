package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.StudentGuardian;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, Long> {
    List<StudentGuardian> findByGuardianId(Long guardianId);
    List<StudentGuardian> findByStudentId(Long studentId);
    Optional<StudentGuardian> findByStudentIdAndGuardianId(Long studentId, Long guardianId);
    boolean existsByStudentIdAndGuardianId(Long studentId, Long guardianId);

    @Query("SELECT sg.guardian FROM StudentGuardian sg WHERE sg.student.id = :studentId")
    List<Parent> findGuardiansByStudentId(@Param("studentId") Long studentId);
}
