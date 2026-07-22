package vn.edu.fpt.myfschool.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.GradeImportRow;

public interface GradeImportRowRepository extends JpaRepository<GradeImportRow, Long> {
    List<GradeImportRow> findByBatchIdOrderBySourceOrderAsc(Long batchId);
    List<GradeImportRow> findByBatchIdAndClsIdOrderBySourceOrderAsc(Long batchId, Long classId);
    Optional<GradeImportRow> findByBatchIdAndStudentId(Long batchId, Long studentId);
}
