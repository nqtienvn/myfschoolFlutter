package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AnnouncementPolicyUpdateRequest(
        @NotNull Long academicYearId,
        boolean enabled,
        @NotBlank @Size(max = 500) String rejectionMessage,
        @NotNull @Size(min = 1, max = 200) List<@Valid AnnouncementPolicyRuleRequest> rules
) {}
