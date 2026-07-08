package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.common.enums.LeaveStatus;
import vn.edu.fpt.myfschool.entity.LeaveRequest;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByParentIdOrderByCreatedAtDesc(Long parentId);
    List<LeaveRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.cls.id = :classId ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByClassIdOrderByCreatedAtDesc(@Param("classId") Long classId);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.cls.id = :classId AND lr.status = :status ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByClassIdAndStatusOrderByCreatedAtDesc(@Param("classId") Long classId,
                                                                   @Param("status") LeaveStatus status);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.cls.id IN :classIds AND lr.status = :status ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByClassIdsAndStatusOrderByCreatedAtDesc(@Param("classIds") List<Long> classIds,
                                                                    @Param("status") LeaveStatus status);

    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE lr.cls.id IN :classIds AND lr.status = :status")
    long countByClassIdsAndStatus(@Param("classIds") List<Long> classIds,
                                  @Param("status") LeaveStatus status);

    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE lr.student.id = :studentId AND lr.status = 'PENDING' AND lr.dateFrom <= :dateTo AND lr.dateTo >= :dateFrom")
    long countOverlappingPending(@Param("studentId") Long studentId, @Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo);
}
