package vn.edu.fpt.myfschool.common.dto;
import java.util.List;
public record StudentTranscriptDto(Long studentId,String studentName,Long academicYearId,Long semesterId,List<TranscriptSubjectDto> subjects) {}
