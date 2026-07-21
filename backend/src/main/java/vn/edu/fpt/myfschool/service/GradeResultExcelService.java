package vn.edu.fpt.myfschool.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.myfschool.common.dto.GradeImportResultDto;

public interface GradeResultExcelService {
    byte[] createTemplate(Long academicYearId, Long semesterId, Long classId, Long subjectId);
    GradeImportResultDto importScores(Long academicYearId, Long semesterId, Long classId,
                                      Long subjectId, MultipartFile file);
    byte[] exportResults(Long academicYearId, Long semesterId, Long classId);
}
