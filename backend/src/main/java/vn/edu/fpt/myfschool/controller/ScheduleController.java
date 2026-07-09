package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.ClassScheduleDto;
import vn.edu.fpt.myfschool.common.dto.ScheduleDto;
import vn.edu.fpt.myfschool.common.dto.ScheduleRequest;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.service.ScheduleService;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules", description = "Thời khóa biểu")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/class")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "TKB lớp")
    public ResponseEntity<ApiResponse<ClassScheduleDto>> getClassSchedule(
            @RequestParam Long classId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getClassSchedule(classId, semesterId)));
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "TKB giáo viên")
    public ResponseEntity<ApiResponse<ClassScheduleDto>> getTeacherSchedule(
            @RequestParam Long teacherId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getTeacherSchedule(teacherId, semesterId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo tiết TKB")
    public ResponseEntity<ApiResponse<ScheduleDto>> createSchedule(
            @Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo thành công", scheduleService.createSchedule(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa tiết TKB")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công", null));
    }

    @GetMapping("/available-periods")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Tiết trống")
    public ResponseEntity<ApiResponse<List<Integer>>> getAvailablePeriods(
            @RequestParam Long classId, @RequestParam Long semesterId,
            @RequestParam Integer dayOfWeek, @RequestParam Shift shift) {
        return ResponseEntity.ok(ApiResponse.success(
                scheduleService.getAvailablePeriods(classId, semesterId, dayOfWeek, shift)));
    }
}
