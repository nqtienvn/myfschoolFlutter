package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.StudentReviewAudit;

import java.util.List;

public interface StudentReviewAuditRepository extends JpaRepository<StudentReviewAudit, Long> {
    List<StudentReviewAudit> findByEntityTypeAndEntityIdOrderByChangedAtDesc(String entityType, Long entityId);
}
