package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.myfschool.common.enums.AnnouncementPolicyMatchType;
import vn.edu.fpt.myfschool.common.enums.AnnouncementPolicyScope;

public record AnnouncementPolicyRuleRequest(
        @NotBlank @Size(max = 250) String phrase,
        @NotNull AnnouncementPolicyScope scope,
        @NotNull AnnouncementPolicyMatchType matchType
) {}
