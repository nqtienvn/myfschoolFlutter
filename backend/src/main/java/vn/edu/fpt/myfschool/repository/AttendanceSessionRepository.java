package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.controller.entity.AttendanceSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    List<AttendanceSession> findByClsIdAndDateAndShift(Long classId, LocalDate date, Shift shift);
    Optional<AttendanceSession> findByClsIdAndDateAndShiftAndScheduleId(
        Long classId, LocalDate date, Shift shift, Long scheduleId);
    Optional<AttendanceSession> findTopByClsIdAndDateAndShiftOrderByCreatedAtDesc(
        Long classId, LocalDate date, Shift shift);
}