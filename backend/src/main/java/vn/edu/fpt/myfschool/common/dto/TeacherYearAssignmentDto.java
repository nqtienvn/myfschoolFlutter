package vn.edu.fpt.myfschool.common.dto;

public record TeacherYearAssignmentDto(
        Long id,
        Long classId,
        String className,
        Long subjectId,
        String subjectName,
        String subjectCode) {
}
