package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.service.ScheduleService;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules", description = "Thời khóa biểu")
@SecurityRequirement(name = "Bearer Authentication")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/class")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "TKB lớp")
    public ResponseEntity<ApiResponse<ClassScheduleDto>> getClassSchedule(
            @RequestParam Long classId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getClassSchedule(classId, semesterId)));
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "TKB giáo viên")
    public ResponseEntity<ApiResponse<ClassScheduleDto>> getTeacherSchedule(
            @RequestParam Long teacherId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getTeacherSchedule(teacherId, semesterId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Tạo tiết TKB")
    public ResponseEntity<ApiResponse<ScheduleDto>> createSchedule(
            @Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo thành công", scheduleService.createSchedule(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Xóa tiết TKB")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công", null));
    }

    @GetMapping("/available-periods")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Tiết trống")
    public ResponseEntity<ApiResponse<List<Integer>>> getAvailablePeriods(
            @RequestParam Long classId, @RequestParam Long semesterId,
            @RequestParam Integer dayOfWeek, @RequestParam Shift shift) {
        return ResponseEntity.ok(ApiResponse.success(
            scheduleService.getAvailablePeriods(classId, semesterId, dayOfWeek, shift)));
    }
}
