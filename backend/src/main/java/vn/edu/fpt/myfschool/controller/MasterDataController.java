package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;

import vn.edu.fpt.myfschool.common.dto.PeriodDto;
import vn.edu.fpt.myfschool.common.dto.SchoolShiftDto;
import vn.edu.fpt.myfschool.service.MasterDataService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/master-data")
@RequiredArgsConstructor
@Tag(name = "Master Data", description = "Danh mục dùng chung")
public class MasterDataController {

    private final MasterDataService masterDataService;


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
    @Operation(summary = "Khởi tạo danh mục ca, tiết và môn học mặc định")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> initialize() {
        return ResponseEntity.ok(ApiResponse.success(
            "Khởi tạo danh mục dùng chung thành công", masterDataService.initialize()));
    }

}
