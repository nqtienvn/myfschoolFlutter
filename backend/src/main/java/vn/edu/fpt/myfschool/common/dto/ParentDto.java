package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record ParentDto(
    Long id,
    String address,
    String occupation,
    List<StudentSummaryDto> children
) {}
