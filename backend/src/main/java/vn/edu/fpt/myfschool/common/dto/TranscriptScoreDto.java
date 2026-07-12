package vn.edu.fpt.myfschool.common.dto;
import java.math.BigDecimal;
public record TranscriptScoreDto(Long gradeItemId,String code,String name,Integer weight,BigDecimal score) {}
