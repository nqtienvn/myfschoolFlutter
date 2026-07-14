package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.Gender;
import vn.edu.fpt.myfschool.common.enums.UserStatus;

import java.time.LocalDate;

public record ParentChildDto(
    Long id,
    String name,
    String studentCode,
    String className,
    Long classId,
    String schoolName,
    String academicYearName,
    String homeroomTeacherName,
    String homeroomTeacherPhone,
    LocalDate dateOfBirth,
    Gender gender,
    String address,
    String email,
    UserStatus status
) {}
