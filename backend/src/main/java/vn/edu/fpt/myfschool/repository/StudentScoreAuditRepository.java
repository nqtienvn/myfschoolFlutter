package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.StudentScoreAudit;

public interface StudentScoreAuditRepository extends JpaRepository<StudentScoreAudit,Long> {}
