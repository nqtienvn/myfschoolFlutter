package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.ImportResultDto;
import vn.edu.fpt.myfschool.service.EnrollmentImportService;
import vn.edu.fpt.myfschool.service.ScheduleImportService;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Tag(name = "Import Bulk", description = "Nhập dữ liệu hàng loạt từ Excel")
@SecurityRequirement(name = "Bearer Authentication")
public class ImportController {

    private final EnrollmentImportService enrollmentImportService;
    private final ScheduleImportService scheduleImportService;

    @PostMapping("/enrollments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Import đăng ký học sinh vào lớp từ file Excel")
    public ResponseEntity<ApiResponse<ImportResultDto>> importEnrollments(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long academicYearId) {
        ImportResultDto response = enrollmentImportService.importFromExcel(file, academicYearId);
        return ResponseEntity.ok(ApiResponse.success("Nhập danh sách đăng ký thành công", response));
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Import thời khóa biểu từ file Excel")
    public ResponseEntity<ApiResponse<ImportResultDto>> importSchedules(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long semesterId) {
        ImportResultDto response = scheduleImportService.importFromExcel(file, semesterId);
        return ResponseEntity.ok(ApiResponse.success("Nhập thời khóa biểu thành công", response));
    }
}
