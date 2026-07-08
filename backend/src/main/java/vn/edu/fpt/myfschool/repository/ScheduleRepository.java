package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.controller.entity.Schedule;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("SELECT s FROM Schedule s WHERE s.cls.id = :classId AND s.semester.id = :semesterId ORDER BY s.dayOfWeek ASC, s.period ASC")
    List<Schedule> findByClassIdAndSemesterIdOrderByDayOfWeekAscPeriodAsc(@Param("classId") Long classId, @Param("semesterId") Long semesterId);

    List<Schedule> findByTeacherIdAndSemesterIdOrderByDayOfWeekAscPeriodAsc(Long teacherId, Long semesterId);

    @Query("SELECT s FROM Schedule s WHERE s.cls.id = :classId AND s.semester.id = :semesterId AND s.dayOfWeek = :dayOfWeek AND s.period = :period")
    Optional<Schedule> findByClassIdAndSemesterIdAndDayOfWeekAndPeriod(@Param("classId") Long classId, @Param("semesterId") Long semesterId, @Param("dayOfWeek") Integer dayOfWeek, @Param("period") Integer period);

    @Query("SELECT s FROM Schedule s WHERE s.teacher.id = :teacherId AND s.semester.id = :semesterId AND s.dayOfWeek = :dayOfWeek AND s.period = :period")
    Optional<Schedule> findTeacherConflict(@Param("teacherId") Long teacherId, @Param("semesterId") Long semesterId, @Param("dayOfWeek") Integer dayOfWeek, @Param("period") Integer period);

    @Modifying
    @Query("DELETE FROM Schedule s WHERE s.cls.id = :classId AND s.semester.id = :semesterId")
    void deleteByClassIdAndSemesterId(@Param("classId") Long classId, @Param("semesterId") Long semesterId);
}
