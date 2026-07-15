package vn.edu.fpt.myfschool.common.dto;
import java.math.BigDecimal;
import vn.edu.fpt.myfschool.common.enums.AssessmentType;
public record TranscriptScoreDto(
    Long gradeItemId,String code,String name,Integer weight,
    AssessmentType assessmentType,BigDecimal score,String comment,Boolean isGraded
) {}
