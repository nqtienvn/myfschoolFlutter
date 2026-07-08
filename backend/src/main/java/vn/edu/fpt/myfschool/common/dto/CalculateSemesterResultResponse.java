package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record CalculateSemesterResultResponse(
    int processed,
    int updated,
    int skipped,
    List<String> warnings
) {}