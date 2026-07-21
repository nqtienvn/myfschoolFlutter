package vn.edu.fpt.myfschool.common.dto;

public record AnnouncementViolationDto(
        Long ruleId,
        String field,
        String phrase
) {}
