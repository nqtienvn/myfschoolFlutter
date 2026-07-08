package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record ImportResultDto(
    int total,
    int success,
    int failed,
    List<String> errors
) {}
