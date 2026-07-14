package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record TeacherTuitionSummaryDto(
    Long classId,
    String className,
    Long semesterId,
    String semesterName,
    int totalStudents,
    int paidStudents,
    int outstandingStudents,
    int studentsWithoutBills,
    List<TeacherTuitionStudentDto> students
) {}
