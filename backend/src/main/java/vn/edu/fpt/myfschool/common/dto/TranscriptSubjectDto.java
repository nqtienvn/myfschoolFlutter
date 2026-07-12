package vn.edu.fpt.myfschool.common.dto;
import java.math.BigDecimal; import java.util.List;
public record TranscriptSubjectDto(Long subjectId,String subjectName,List<TranscriptScoreDto> scores,BigDecimal average,boolean complete) {}
