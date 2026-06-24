package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.Schedule;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByClassIdAndSemesterIdOrderByDayOfWeekAscPeriodAsc(Long classId, Long semesterId);
    List<Schedule> findByTeacherIdAndSemesterIdOrderByDayOfWeekAscPeriodAsc(Long teacherId, Long semesterId);

    Optional<Schedule> findByClassIdAndSemesterIdAndDayOfWeekAndPeriod(
        Long classId, Long semesterId, Integer dayOfWeek, Integer period);

    @Query("SELECT s FROM Schedule s WHERE s.teacher.id = :teacherId AND s.semester.id = :semesterId " +
           "AND s.dayOfWeek = :dayOfWeek AND s.period = :period")
    Optional<Schedule> findTeacherConflict(@Param("teacherId") Long teacherId,
                                            @Param("semesterId") Long semesterId,
                                            @Param("dayOfWeek") Integer dayOfWeek,
                                            @Param("period") Integer period);

    void deleteByClassIdAndSemesterId(Long classId, Long semesterId);
}
