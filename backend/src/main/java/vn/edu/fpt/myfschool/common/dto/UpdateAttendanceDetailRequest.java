package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateAttendanceDetailRequest(
    @NotNull Long sessionId,
    @NotNull List<UpdateAttendanceDetailEntry> entries
) {}