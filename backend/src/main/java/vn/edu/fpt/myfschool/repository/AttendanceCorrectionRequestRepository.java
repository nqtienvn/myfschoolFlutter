package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.common.enums.AttendanceCorrectionStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.entity.AttendanceCorrectionRequest;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceCorrectionRequestRepository
        extends JpaRepository<AttendanceCorrectionRequest, Long> {
    boolean existsByClsIdAndDateAndShiftAndStatus(
        Long classId, LocalDate date, Shift shift, AttendanceCorrectionStatus status);

    List<AttendanceCorrectionRequest> findByClsAcademicYearIdAndDateAndStatusOrderByCreatedAtAsc(
        Long academicYearId, LocalDate date, AttendanceCorrectionStatus status);

    List<AttendanceCorrectionRequest> findByTeacherIdAndClsAcademicYearIdOrderByCreatedAtDesc(
        Long teacherId, Long academicYearId);

    List<AttendanceCorrectionRequest> findByClsAcademicYearIdAndDateOrderByCreatedAtDesc(
        Long academicYearId, LocalDate date);
}
