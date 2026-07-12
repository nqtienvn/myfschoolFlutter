package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import java.util.List;

public interface GradeConfigurationService {
    List<GradeConfigDto> listTemplates();
    GradeConfigDto createTemplate(CreateGradeConfigTemplateRequest request);
    GradeConfigDto getYearConfig(Long academicYearId);
    void copyToYear(Long academicYearId, Long templateId, List<GradeConfigItemRequest> inlineItems);
}
