package vn.edu.fpt.myfschool.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.myfschool.common.dto.ImportResultDto;

public interface EnrollmentImportService {
    ImportResultDto importFromExcel(MultipartFile file, Long academicYearId);
}
