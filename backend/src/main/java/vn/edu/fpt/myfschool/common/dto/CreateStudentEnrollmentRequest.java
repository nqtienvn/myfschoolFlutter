package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.*;
import vn.edu.fpt.myfschool.common.enums.Gender;
import vn.edu.fpt.myfschool.common.enums.Relationship;
import java.time.LocalDate;

public record CreateStudentEnrollmentRequest(
    @NotNull Long academicYearId,
    @NotNull Long classId,
    @NotBlank @Size(max = 20) String studentCode,
    @NotBlank @Size(max = 100) String studentName,
    @NotNull @Past LocalDate dateOfBirth,
    @NotNull Gender gender,
    @Size(max = 500) String studentAddress,
    @Size(max = 30) String studentCitizenId,
    @NotBlank @Email @Size(max = 255) String studentEmail,
    @NotBlank @Size(max = 100) String parentName,
    @NotNull Relationship relationship,
    @NotBlank @Pattern(regexp = "0[0-9]{9}") String parentPhone,
    @Email String parentEmail,
    @Size(max = 30) String parentCitizenId,
    @Size(max = 200) String parentOccupation,
    @Size(max = 500) String parentAddress
) {}
