package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.UserStatus;

import java.util.List;

public record TeacherSummaryDto(
    Long id,
    Long userId,
    String phone,
    String name,
    String email,
    UserStatus status,
    String employeeCode,
    String avatar,
    List<SubjectDto> subjects
) {}
