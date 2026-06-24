package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record AttendanceLogDto(List<AttendanceDto> records, AttendanceStatsDto stats) {}
