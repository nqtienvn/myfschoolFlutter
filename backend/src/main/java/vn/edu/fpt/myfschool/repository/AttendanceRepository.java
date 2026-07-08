package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.entity.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query("SELECT a FROM Attendance a WHERE a.cls.id = :classId AND a.date = :date AND a.shift = :shift")
    List<Attendance> findByClsIdAndDateAndShift(@Param("classId") Long classId, @Param("date") LocalDate date, @Param("shift") Shift shift);

    List<Attendance> findByStudentIdOrderByDateDesc(Long studentId);

    Optional<Attendance> findByStudentIdAndDateAndShift(Long studentId, LocalDate date, Shift shift);

    @Query("SELECT a.status, COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.date BETWEEN :startDate AND :endDate GROUP BY a.status")
    List<Object[]> countByStatusBetweenDates(@Param("studentId") Long studentId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    long countByStudentIdAndStatusAndDateBetween(Long studentId, AttendanceStatus status, LocalDate startDate, LocalDate endDate);
}
