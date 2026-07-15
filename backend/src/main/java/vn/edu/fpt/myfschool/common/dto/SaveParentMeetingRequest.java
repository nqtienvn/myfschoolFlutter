package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.*;
import vn.edu.fpt.myfschool.common.enums.ParentMeetingStatus;

import java.time.LocalDateTime;

public record SaveParentMeetingRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull Long academicYearId,
        @NotNull Long semesterId,
        @NotNull Long classId,
        Long studentId,
        @NotNull LocalDateTime startsAt,
        @Size(max = 300) String location,
        @Size(max = 2000) String agenda,
        ParentMeetingStatus status
) {}
