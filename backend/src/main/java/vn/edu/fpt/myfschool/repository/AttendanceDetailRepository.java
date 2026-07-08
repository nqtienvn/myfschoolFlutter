package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.controller.entity.AttendanceDetail;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceDetailRepository extends JpaRepository<AttendanceDetail, Long> {
    List<AttendanceDetail> findBySessionId(Long sessionId);
    Optional<AttendanceDetail> findBySessionIdAndStudentId(Long sessionId, Long studentId);
    long countBySessionIdAndStatus(Long sessionId, AttendanceStatus status);
}