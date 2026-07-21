package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AnnouncementPolicyMatchType;
import vn.edu.fpt.myfschool.common.enums.AnnouncementPolicyScope;

public record AnnouncementPolicyRuleDto(
        Long id,
        String phrase,
        AnnouncementPolicyScope scope,
        AnnouncementPolicyMatchType matchType
) {}
