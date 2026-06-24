package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.LeaveRequest;
import vn.edu.fpt.myfschool.common.enums.LeaveStatus;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByParentIdOrderByCreatedAtDesc(Long parentId);
    List<LeaveRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<LeaveRequest> findByClassIdOrderByCreatedAtDesc(Long classId);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.cls.id = :classId AND lr.status = 'PENDING' ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findPendingByClass(@Param("classId") Long classId);

    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE lr.cls.id IN :classIds AND lr.status = 'PENDING'")
    long countPendingByClassIds(@Param("classIds") List<Long> classIds);

    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE lr.student.id = :studentId AND lr.status = 'PENDING' " +
           "AND lr.dateFrom <= :dateTo AND lr.dateTo >= :dateFrom")
    long countOverlappingPending(@Param("studentId") Long studentId,
                                  @Param("dateFrom") LocalDate dateFrom,
                                  @Param("dateTo") LocalDate dateTo);
}
