package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import java.util.List;

public interface HomeroomAssignmentService {
    List<HomeroomAssignmentDto> listByClass(Long classId, Long academicYearId);
    HomeroomAssignmentDto getById(Long id);
    HomeroomAssignmentDto getByClassAndYear(Long classId, Long academicYearId);
    HomeroomAssignmentDto create(CreateHomeroomAssignmentRequest request);
    HomeroomAssignmentDto update(Long id, CreateHomeroomAssignmentRequest request);
    void delete(Long id);
}
