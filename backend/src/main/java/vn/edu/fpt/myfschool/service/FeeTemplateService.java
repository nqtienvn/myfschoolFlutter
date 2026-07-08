package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import java.util.List;

public interface FeeTemplateService {
    List<FeeTemplateDto> listByClass(Long classId, Long semesterId);
    FeeTemplateDto create(CreateFeeTemplateRequest request);
    GenerateBillResultDto generateBills(Long feeTemplateId);
    void delete(Long id);
}