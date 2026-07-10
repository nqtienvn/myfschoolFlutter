package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.Schedule;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByAssignmentId(Long assignmentId);

    @Query("SELECT s FROM Schedule s WHERE s.assignment.cls.id = :classId " +
           "AND s.assignment.semester.id = :semesterId " +
           "ORDER BY s.dayOfWeek ASC, s.period ASC")
    List<Schedule> findByClassIdAndSemesterId(@Param("classId") Long classId,
                                               @Param("semesterId") Long semesterId);

    @Query("SELECT s FROM Schedule s WHERE s.assignment.teacher.id = :teacherId " +
           "AND s.assignment.semester.id = :semesterId " +
           "ORDER BY s.dayOfWeek ASC, s.period ASC")
    List<Schedule> findByTeacherIdAndSemesterId(@Param("teacherId") Long teacherId,
                                                 @Param("semesterId") Long semesterId);

    Optional<Schedule> findByAssignmentIdAndDayOfWeekAndPeriod(
        Long assignmentId, Integer dayOfWeek, Integer period);

    @Query("SELECT s FROM Schedule s WHERE s.assignment.cls.id = :classId " +
           "AND s.assignment.semester.id = :semesterId " +
           "AND s.dayOfWeek = :dayOfWeek AND s.period = :period")
    Optional<Schedule> findByClassIdAndSemesterIdAndDayOfWeekAndPeriod(
        @Param("classId") Long classId, @Param("semesterId") Long semesterId,
        @Param("dayOfWeek") Integer dayOfWeek, @Param("period") Integer period);

    @Query("SELECT s FROM Schedule s WHERE s.assignment.teacher.id = :teacherId " +
           "AND s.assignment.semester.id = :semesterId " +
           "AND s.dayOfWeek = :dayOfWeek AND s.period = :period")
    Optional<Schedule> findTeacherConflict(@Param("teacherId") Long teacherId,
        @Param("semesterId") Long semesterId, @Param("dayOfWeek") Integer dayOfWeek,
        @Param("period") Integer period);

    @Modifying
    @Query("DELETE FROM Schedule s WHERE s.assignment.cls.id = :classId " +
           "AND s.assignment.semester.id = :semesterId")
    void deleteByClassIdAndSemesterId(@Param("classId") Long classId,
                                       @Param("semesterId") Long semesterId);

    @Modifying
    @Query("DELETE FROM Schedule s WHERE s.assignment.cls.id = :classId")
    void deleteByClassId(@Param("classId") Long classId);
}
