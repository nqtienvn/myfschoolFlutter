package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.GradeLevelDto;
import vn.edu.fpt.myfschool.common.dto.PeriodDto;
import vn.edu.fpt.myfschool.common.dto.SchoolShiftDto;
import vn.edu.fpt.myfschool.service.MasterDataService;

import java.util.List;

@RestController
@RequestMapping("/api/master-data")
@RequiredArgsConstructor
@Tag(name = "Master Data", description = "Danh mục dùng chung")
@SecurityRequirement(name = "Bearer Authentication")
public class MasterDataController {

    private final MasterDataService masterDataService;

    @GetMapping("/grade-levels")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @Operation(summary = "Lấy danh sách khối lớp")
    public ResponseEntity<ApiResponse<List<GradeLevelDto>>> getGradeLevels() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khối lớp thành công", masterDataService.getGradeLevels()));
    }

    @GetMapping("/shifts")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @Operation(summary = "Lấy danh sách ca học")
    public ResponseEntity<ApiResponse<List<SchoolShiftDto>>> getShifts() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ca học thành công", masterDataService.getShifts()));
    }

    @GetMapping("/periods")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @Operation(summary = "Lấy danh sách tiết học")
    public ResponseEntity<ApiResponse<List<PeriodDto>>> getPeriods(
            @RequestParam(required = false) Long shiftId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tiết học thành công", masterDataService.getPeriods(shiftId)));
    }

    @PostMapping("/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Khởi tạo nhanh dữ liệu danh mục mẫu")
    public ResponseEntity<ApiResponse<Void>> initialize() {
        masterDataService.initializeMasterData();
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo danh mục mẫu thành công", null));
    }
}
