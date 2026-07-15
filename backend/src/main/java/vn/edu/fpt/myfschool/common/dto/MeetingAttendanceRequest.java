package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import vn.edu.fpt.myfschool.common.enums.MeetingAttendance;

public record MeetingAttendanceRequest(
        @NotNull Long guardianId,
        @NotNull MeetingAttendance attendance
) {}
