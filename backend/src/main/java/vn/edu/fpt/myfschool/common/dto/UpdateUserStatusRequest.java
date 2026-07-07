package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import vn.edu.fpt.myfschool.common.enums.UserStatus;

public record UpdateUserStatusRequest(
    @NotNull UserStatus status
) {}
