package vn.edu.fpt.myfschool.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.myfschool.common.dto.AdminUserDto;
import vn.edu.fpt.myfschool.common.dto.CreateTeacherAccountRequest;
import vn.edu.fpt.myfschool.common.dto.TeacherAccountCredentialDto;
import vn.edu.fpt.myfschool.common.dto.TeacherManagementSummaryDto;
import vn.edu.fpt.myfschool.common.dto.TeacherSummaryDto;
import vn.edu.fpt.myfschool.common.dto.UpdateTeacherProfileRequest;
import vn.edu.fpt.myfschool.common.dto.UpdateTeacherSubjectsRequest;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;

public interface AdminUserService {
    Page<AdminUserDto> listUsers(UserRole role, UserStatus status, String keyword, int page, int size);
    AdminUserDto updateUserStatus(Long userId, UserStatus status);
    TeacherAccountCredentialDto createTeacherAccount(CreateTeacherAccountRequest request);
    Page<TeacherSummaryDto> listTeachers(UserStatus status, String keyword, Long subjectId,
                                         Long academicYearId, int page, int size);
    TeacherManagementSummaryDto getTeacherSummary(Long academicYearId);
    TeacherSummaryDto updateTeacherProfile(Long teacherId, UpdateTeacherProfileRequest request,
                                            Long academicYearId);
    TeacherSummaryDto updateTeacherSubjects(Long teacherId, UpdateTeacherSubjectsRequest request);
    TeacherAccountCredentialDto resetTeacherPassword(Long teacherId);
}
