package vn.edu.fpt.myfschool.common.dto;

public record TeacherAccountCredentialDto(
        TeacherSummaryDto teacher,
        String temporaryPassword) {
}
