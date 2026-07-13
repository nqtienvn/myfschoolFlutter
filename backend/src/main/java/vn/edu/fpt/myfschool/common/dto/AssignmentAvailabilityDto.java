package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record AssignmentAvailabilityDto(
    Integer dayOfWeek,
    Long periodId,
    List<Long> assignmentIds
) {}
