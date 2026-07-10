package vn.edu.fpt.myfschool.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDto(
    Long id,
    String phone,
    String name,
    String email,
    String avatar,
    UserRole role,
    UserStatus status,
    LocalDateTime createdAt,
    Boolean mustChangePassword,
    ParentDto parentProfile,
    StudentDto studentProfile,
    TeacherDto teacherProfile,
    UserSettingDto settings
) {}
