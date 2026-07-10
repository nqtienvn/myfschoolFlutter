package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.Relationship;

import java.util.List;

public record StudentAccountByClassDto(
    Long studentId,
    String studentCode,
    String studentName,
    String studentUsername,
    List<GuardianAccountDto> guardians
) {
    public record GuardianAccountDto(
        Long parentId,
        String parentName,
        String parentUsername,
        String parentEmail,
        Relationship relationship
    ) {}
}
