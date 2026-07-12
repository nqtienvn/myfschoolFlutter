package vn.edu.fpt.myfschool.service;
import vn.edu.fpt.myfschool.common.dto.StudentTranscriptDto;
public interface TranscriptService {
    StudentTranscriptDto get(Long studentId,Long academicYearId,Long semesterId);
    StudentTranscriptDto getMine(Long academicYearId,Long semesterId);
}
