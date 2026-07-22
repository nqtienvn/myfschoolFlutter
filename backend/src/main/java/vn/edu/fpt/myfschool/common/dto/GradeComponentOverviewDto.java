package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record GradeComponentOverviewDto(
        List<GradeComponentColumnDto> columns,
        List<GradeComponentOverviewRowDto> rows
) {}
