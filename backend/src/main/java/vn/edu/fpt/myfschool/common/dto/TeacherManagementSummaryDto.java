package vn.edu.fpt.myfschool.common.dto;

public record TeacherManagementSummaryDto(
        long total,
        long active,
        long locked,
        long unassigned,
        long homeroom) {
}
