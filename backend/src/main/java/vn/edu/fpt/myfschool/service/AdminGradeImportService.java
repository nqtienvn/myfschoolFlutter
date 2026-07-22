package vn.edu.fpt.myfschool.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportBatchDto;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportContextDto;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportItemDto;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportResultDto;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportTableDto;
import vn.edu.fpt.myfschool.common.dto.UpdateAdminGradeImportRowRequest;

public interface AdminGradeImportService {
    AdminGradeImportContextDto getContext();
    List<AdminGradeImportItemDto> getItems();
    byte[] createTemplate(String itemCode);
    AdminGradeImportResultDto importFile(MultipartFile file);
    List<AdminGradeImportBatchDto> getBatches();
    AdminGradeImportTableDto getBatch(Long batchId, Long classId);
    AdminGradeImportTableDto updateRow(Long batchId, Long studentId, UpdateAdminGradeImportRowRequest request);
}
