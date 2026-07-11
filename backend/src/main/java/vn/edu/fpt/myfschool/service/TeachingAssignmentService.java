package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import java.util.List;

public interface TeachingAssignmentService {
    List<TeachingAssignmentDto> listByClass(Long classId, AssignmentStatus status);
    List<TeachingAssignmentDto> listByTeacher(Long teacherId, AssignmentStatus status);
    TeachingAssignmentDto getById(Long id);
    TeachingAssignmentDetailDto getDetail(Long id);
    TeachingAssignmentDto create(CreateTeachingAssignmentRequest request);
    TeachingAssignmentDto update(Long id, CreateTeachingAssignmentRequest request);
    TeachingAssignmentDto deactivate(Long id);
    TeachingAssignmentDto reactivate(Long id);
}
