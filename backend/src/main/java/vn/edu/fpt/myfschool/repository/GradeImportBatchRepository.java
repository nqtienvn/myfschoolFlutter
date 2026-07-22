package vn.edu.fpt.myfschool.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.GradeImportBatch;

public interface GradeImportBatchRepository extends JpaRepository<GradeImportBatch, Long> {
    List<GradeImportBatch> findBySemesterIdOrderByCreatedAtDesc(Long semesterId);
}
