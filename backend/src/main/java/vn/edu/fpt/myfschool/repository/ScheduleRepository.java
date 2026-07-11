package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.Schedule;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Schedule s " +
           "WHERE s.timetable.cls.academicYear.id = :academicYearId AND s.periodRef.id IN :periodIds")
    boolean existsInAcademicYearByPeriodIds(@Param("academicYearId") Long academicYearId,
                                             @Param("periodIds") Collection<Long> periodIds);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Schedule s " +
           "WHERE s.timetable.cls.academicYear.id = :academicYearId AND s.periodRef.shift.id IN :shiftIds")
    boolean existsInAcademicYearByShiftIds(@Param("academicYearId") Long academicYearId,
                                            @Param("shiftIds") Collection<Long> shiftIds);

    List<Schedule> findByAssignmentId(Long assignmentId);

    List<Schedule> findByTimetableIdOrderByDayOfWeekAscPeriodAsc(Long timetableId);

    @Query("SELECT s FROM Schedule s WHERE s.timetable.cls.id = :classId " +
           "AND s.timetable.semester.id = :semesterId AND s.timetable.status = 'ACTIVE' " +
           "ORDER BY s.dayOfWeek ASC, s.period ASC")
    List<Schedule> findByClassIdAndSemesterId(@Param("classId") Long classId,
                                               @Param("semesterId") Long semesterId);

    @Query("SELECT s FROM Schedule s WHERE s.assignment.teacher.id = :teacherId " +
           "AND s.timetable.semester.id = :semesterId AND s.timetable.status = 'ACTIVE' " +
           "ORDER BY s.dayOfWeek ASC, s.period ASC")
    List<Schedule> findByTeacherIdAndSemesterId(@Param("teacherId") Long teacherId,
                                                 @Param("semesterId") Long semesterId);

    Optional<Schedule> findByTimetableIdAndDayOfWeekAndPeriodRefId(
        Long timetableId, Integer dayOfWeek, Long periodId);

    @Query("SELECT s FROM Schedule s WHERE s.timetable.cls.id = :classId " +
           "AND s.timetable.semester.id = :semesterId AND s.timetable.status = 'ACTIVE' " +
           "AND s.dayOfWeek = :dayOfWeek AND s.periodRef.id = :periodId")
    Optional<Schedule> findByClassIdAndSemesterIdAndDayOfWeekAndPeriodId(
        @Param("classId") Long classId, @Param("semesterId") Long semesterId,
        @Param("dayOfWeek") Integer dayOfWeek, @Param("periodId") Long periodId);

    @Query("SELECT s FROM Schedule s WHERE s.assignment.teacher.id = :teacherId " +
           "AND s.timetable.semester.id = :semesterId AND s.timetable.status = 'ACTIVE' " +
           "AND s.dayOfWeek = :dayOfWeek AND s.periodRef.id = :periodId")
    Optional<Schedule> findTeacherConflict(@Param("teacherId") Long teacherId,
        @Param("semesterId") Long semesterId, @Param("dayOfWeek") Integer dayOfWeek,
        @Param("periodId") Long periodId);

    @Modifying
    @Query("DELETE FROM Schedule s WHERE s.timetable.cls.id = :classId " +
           "AND s.timetable.semester.id = :semesterId")
    void deleteByClassIdAndSemesterId(@Param("classId") Long classId,
                                       @Param("semesterId") Long semesterId);

    @Modifying
    @Query("DELETE FROM Schedule s WHERE s.timetable.cls.id = :classId")
    void deleteByClassId(@Param("classId") Long classId);
}
