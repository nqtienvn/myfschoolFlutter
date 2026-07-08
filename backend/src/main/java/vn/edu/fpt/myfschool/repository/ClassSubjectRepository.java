package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.controller.entity.ClassSubject;
import vn.edu.fpt.myfschool.controller.entity.Subject;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSubjectRepository extends JpaRepository<ClassSubject, Long> {

    List<ClassSubject> findByClsIdAndAcademicYear(Long classId, String academicYear);

    List<ClassSubject> findByTeacherIdAndAcademicYear(Long teacherId, String academicYear);

    Optional<ClassSubject> findByClsIdAndSubjectIdAndAcademicYear(
        Long classId, Long subjectId, String academicYear);

    @Query("SELECT DISTINCT cs.cls.id FROM ClassSubject cs WHERE cs.teacher.id = :teacherId")
    List<Long> findClassIdsByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT CASE WHEN COUNT(cs) > 0 THEN true ELSE false END FROM ClassSubject cs " +
           "WHERE cs.teacher.id = :teacherId AND cs.cls.id = :classId")
    boolean existsByTeacherIdAndClassId(@Param("teacherId") Long teacherId,
                                        @Param("classId") Long classId);

    @Query("SELECT DISTINCT cs.subject FROM ClassSubject cs " +
           "WHERE cs.teacher.id = :teacherId AND cs.academicYear = :year")
    List<Subject> findSubjectsByTeacher(@Param("teacherId") Long teacherId,
                                         @Param("year") String year);
}
