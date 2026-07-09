package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.AcademicYearArchiveStatsDto;
import vn.edu.fpt.myfschool.common.dto.AcademicYearDto;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.CreateAcademicYearRequest;
import vn.edu.fpt.myfschool.common.dto.UpdateAcademicYearRequest;
import vn.edu.fpt.myfschool.common.dto.UpdateAcademicYearStatusRequest;
import vn.edu.fpt.myfschool.service.AcademicYearService;

import java.util.List;

@RestController
@RequestMapping("/api/academic-years")
@RequiredArgsConstructor
@Tag(name = "Academic Years", description = "Quản lý năm học")
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Danh sách năm học")
    public ResponseEntity<ApiResponse<List<AcademicYearDto>>> listAcademicYears() {
        return ResponseEntity.ok(ApiResponse.success(academicYearService.listAcademicYears()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo năm học")
    public ResponseEntity<ApiResponse<AcademicYearDto>> createAcademicYear(
            @Valid @RequestBody CreateAcademicYearRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo năm học thành công", academicYearService.createAcademicYear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa năm học")
    public ResponseEntity<ApiResponse<AcademicYearDto>> updateAcademicYear(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAcademicYearRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật năm học thành công", academicYearService.updateAcademicYear(id, request)));
    }

    @PostMapping("/{id}/open")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mở năm học")
    public ResponseEntity<ApiResponse<Void>> openAcademicYear(@PathVariable Long id) {
        academicYearService.openAcademicYear(id);
        return ResponseEntity.ok(ApiResponse.success("Mở năm học thành công", null));
    }

    @PostMapping("/{id}/open-hk2")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mở học kỳ 2")
    public ResponseEntity<ApiResponse<Void>> openSemester2(@PathVariable Long id) {
        academicYearService.openSemester2(id);
        return ResponseEntity.ok(ApiResponse.success("Mở học kỳ 2 thành công", null));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kết thúc năm học")
    public ResponseEntity<ApiResponse<Void>> completeAcademicYear(@PathVariable Long id) {
        academicYearService.completeAcademicYear(id);
        return ResponseEntity.ok(ApiResponse.success("Kết thúc năm học thành công", null));
    }

    @GetMapping("/{id}/archive-stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xem thống kê hồ sơ năm học đã kết thúc")
    public ResponseEntity<ApiResponse<AcademicYearArchiveStatsDto>> getArchiveStats(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(academicYearService.getArchiveStats(id)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Đổi trạng thái năm học")
    public ResponseEntity<ApiResponse<AcademicYearDto>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAcademicYearStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", academicYearService.updateStatus(id, request.status())));
    }
}
