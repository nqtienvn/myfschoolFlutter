package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.myfschool.common.enums.AttendanceCorrectionStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.entity.AttendanceCorrectionRequest;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceCorrectionRequestRepository
        extends JpaRepository<AttendanceCorrectionRequest, Long> {
    boolean existsByClsIdAndDateAndShiftAndStatus(
        Long classId, LocalDate date, Shift shift, AttendanceCorrectionStatus status);

    List<AttendanceCorrectionRequest> findByTeacherIdAndClsAcademicYearIdOrderByCreatedAtDesc(
        Long teacherId, Long academicYearId);

    @Query("""
        select request
        from AttendanceCorrectionRequest request
        where request.cls.academicYear.id = :academicYearId
          and (:status is null or request.status = :status)
          and (:date is null or request.date = :date)
          and (:classId is null or request.cls.id = :classId)
          and (:teacherId is null or request.teacher.id = :teacherId)
        order by request.createdAt desc
        """)
    List<AttendanceCorrectionRequest> searchAdminCorrections(
        @Param("academicYearId") Long academicYearId,
        @Param("status") AttendanceCorrectionStatus status,
        @Param("date") LocalDate date,
        @Param("classId") Long classId,
        @Param("teacherId") Long teacherId);

    long countByClsAcademicYearIdAndStatus(
        Long academicYearId, AttendanceCorrectionStatus status);
}
