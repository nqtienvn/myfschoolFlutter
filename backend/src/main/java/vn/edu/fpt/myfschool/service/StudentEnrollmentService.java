package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.CreateStudentEnrollmentRequest;
import vn.edu.fpt.myfschool.common.dto.StudentEnrollmentResultDto;

public interface StudentEnrollmentService {
    StudentEnrollmentResultDto create(CreateStudentEnrollmentRequest request);
}
