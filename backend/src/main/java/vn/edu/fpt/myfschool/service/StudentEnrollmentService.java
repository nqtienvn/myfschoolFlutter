package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.CreateStudentEnrollmentRequest;
import vn.edu.fpt.myfschool.common.dto.StudentAccountByClassDto;
import vn.edu.fpt.myfschool.common.dto.StudentEnrollmentResultDto;

import java.util.List;

public interface StudentEnrollmentService {
    StudentEnrollmentResultDto create(CreateStudentEnrollmentRequest request);
    List<StudentAccountByClassDto> listAccounts(Long academicYearId, Long classId);
}
