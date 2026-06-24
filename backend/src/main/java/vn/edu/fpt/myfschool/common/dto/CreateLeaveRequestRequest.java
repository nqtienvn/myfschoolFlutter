package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import vn.edu.fpt.myfschool.common.enums.LeaveShift;

public record CreateLeaveRequestRequest(
    @NotNull Long studentId, @NotNull LocalDate dateFrom, @NotNull LocalDate dateTo,
    @NotNull LeaveShift shift, @NotBlank String reason
) {}
