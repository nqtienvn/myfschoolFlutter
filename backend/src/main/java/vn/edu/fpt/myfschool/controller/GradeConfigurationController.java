package vn.edu.fpt.myfschool.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.service.GradeConfigurationService;
import java.util.List;

@RestController @RequestMapping("/api/grade-configurations") @RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class GradeConfigurationController {
    private final GradeConfigurationService service;
    @GetMapping("/templates") public ResponseEntity<ApiResponse<List<GradeConfigDto>>> templates(){return ResponseEntity.ok(ApiResponse.success(service.listTemplates()));}
    @PostMapping("/templates") public ResponseEntity<ApiResponse<GradeConfigDto>> create(@Valid @RequestBody CreateGradeConfigTemplateRequest request){return ResponseEntity.ok(ApiResponse.success("Đã tạo mẫu cấu hình",service.createTemplate(request)));}
    @GetMapping("/academic-years/{yearId}") public ResponseEntity<ApiResponse<GradeConfigDto>> year(@PathVariable Long yearId){return ResponseEntity.ok(ApiResponse.success(service.getYearConfig(yearId)));}
    @PostMapping("/academic-years/{yearId}/templates/{templateId}") public ResponseEntity<ApiResponse<GradeConfigDto>> apply(
            @PathVariable Long yearId, @PathVariable Long templateId) {
        return ResponseEntity.ok(ApiResponse.success("Đã áp dụng cấu hình đầu điểm cho năm học", service.applyTemplateToYear(yearId, templateId)));
    }
}
