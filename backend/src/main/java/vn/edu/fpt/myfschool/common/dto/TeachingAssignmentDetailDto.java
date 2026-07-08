package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record TeachingAssignmentDetailDto(
    TeachingAssignmentDto assignment,
    List<ScheduleDto> schedules
) {}