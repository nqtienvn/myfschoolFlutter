package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.AutoGenerateTimetableRequest;
import vn.edu.fpt.myfschool.common.dto.AutoGenerateTimetableResult;

public interface TimetableGenerationService {
    AutoGenerateTimetableResult generate(AutoGenerateTimetableRequest request);
}
