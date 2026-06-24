package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record ClassRankingDto(
    Long classId, String className, Long semesterId, String semesterName,
    List<ClassRankEntryDto> rankings
) {}
