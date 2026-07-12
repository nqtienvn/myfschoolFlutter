package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.*;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.GradeConfigurationService;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class GradeConfigurationServiceImpl implements GradeConfigurationService {
    private final GradeConfigTemplateRepository templateRepository;
    private final AcademicYearGradeConfigRepository yearConfigRepository;
    private final AcademicYearRepository yearRepository;

    @Override @Transactional(readOnly=true)
    public List<GradeConfigDto> listTemplates() { return templateRepository.findByActiveTrueOrderByNameAscVersionDesc().stream().map(this::templateDto).toList(); }

    @Override public GradeConfigDto createTemplate(CreateGradeConfigTemplateRequest request) {
        validate(request.items());
        GradeConfigTemplate template = new GradeConfigTemplate(); template.setName(request.name().trim());
        for (GradeConfigItemRequest input : request.items()) {
            GradeConfigTemplateItem item = new GradeConfigTemplateItem(); copy(input,item); item.setTemplate(template); template.getItems().add(item);
        }
        return templateDto(templateRepository.save(template));
    }

    @Override @Transactional(readOnly=true)
    public GradeConfigDto getYearConfig(Long academicYearId) {
        return yearDto(yearConfigRepository.findByAcademicYearId(academicYearId)
            .orElseThrow(() -> new ResourceNotFoundException("GradeConfiguration","academicYearId",academicYearId)));
    }

    @Override public void copyToYear(Long academicYearId, Long templateId, List<GradeConfigItemRequest> inlineItems) {
        if (yearConfigRepository.existsByAcademicYearId(academicYearId)) throw new ConflictException("Năm học đã có cấu hình điểm");
        AcademicYear year = yearRepository.findById(academicYearId).orElseThrow(() -> new ResourceNotFoundException("AcademicYear","id",academicYearId));
        GradeConfigTemplate source = templateId == null ? null : templateRepository.findById(templateId)
            .orElseThrow(() -> new ResourceNotFoundException("GradeConfigTemplate","id",templateId));
        List<GradeConfigItemRequest> inputs = source == null ? inlineItems : source.getItems().stream().map(this::requestOf).toList();
        validate(inputs);
        AcademicYearGradeConfig config = new AcademicYearGradeConfig(); config.setAcademicYear(year); config.setSourceTemplate(source);
        for (GradeConfigItemRequest input : inputs) { AcademicYearGradeConfigItem item=new AcademicYearGradeConfigItem(); copy(input,item); item.setConfig(config); config.getItems().add(item); }
        yearConfigRepository.save(config);
    }

    private void validate(List<GradeConfigItemRequest> items) {
        if (items == null || items.isEmpty()) throw new BadRequestException("Phải cấu hình ít nhất một đầu điểm");
        Set<String> codes=new HashSet<>(); for(var item:items) if(!codes.add(item.code().trim().toUpperCase())) throw new BadRequestException("Mã đầu điểm bị trùng: "+item.code());
    }
    private void copy(GradeConfigItemRequest r, GradeConfigTemplateItem i){i.setCode(r.code().trim().toUpperCase());i.setDisplayName(r.displayName().trim());i.setWeight(r.weight());i.setQuantity(r.quantity());i.setEntryRole(r.entryRole());i.setAssessmentType(r.assessmentType());i.setRequiredEntry(r.requiredEntry()==null||r.requiredEntry());i.setDisplayOrder(r.displayOrder());}
    private void copy(GradeConfigItemRequest r, AcademicYearGradeConfigItem i){i.setCode(r.code().trim().toUpperCase());i.setDisplayName(r.displayName().trim());i.setWeight(r.weight());i.setQuantity(r.quantity());i.setEntryRole(r.entryRole());i.setAssessmentType(r.assessmentType());i.setRequiredEntry(r.requiredEntry()==null||r.requiredEntry());i.setDisplayOrder(r.displayOrder());}
    private GradeConfigItemRequest requestOf(GradeConfigTemplateItem i){return new GradeConfigItemRequest(i.getCode(),i.getDisplayName(),i.getWeight(),i.getQuantity(),i.getEntryRole(),i.getAssessmentType(),i.getRequiredEntry(),i.getDisplayOrder());}
    private GradeConfigDto templateDto(GradeConfigTemplate t){return new GradeConfigDto(t.getId(),t.getName(),t.getVersion(),null,t.getActive()?"ACTIVE":"ARCHIVED",t.getItems().stream().map(i->new GradeConfigItemDto(i.getId(),i.getCode(),i.getDisplayName(),i.getWeight(),i.getQuantity(),i.getEntryRole(),i.getAssessmentType(),i.getRequiredEntry(),i.getDisplayOrder())).toList());}
    private GradeConfigDto yearDto(AcademicYearGradeConfig c){return new GradeConfigDto(c.getId(),c.getSourceTemplate()==null?"Cấu hình riêng":c.getSourceTemplate().getName(),c.getSourceTemplate()==null?1:c.getSourceTemplate().getVersion(),c.getAcademicYear().getId(),c.getStatus(),c.getItems().stream().map(i->new GradeConfigItemDto(i.getId(),i.getCode(),i.getDisplayName(),i.getWeight(),i.getQuantity(),i.getEntryRole(),i.getAssessmentType(),i.getRequiredEntry(),i.getDisplayOrder())).toList());}
}
