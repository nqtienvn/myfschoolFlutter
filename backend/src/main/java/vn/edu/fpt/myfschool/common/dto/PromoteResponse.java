package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record PromoteResponse(
    int promoted,
    int skipped,
    List<String> warnings
) {}
