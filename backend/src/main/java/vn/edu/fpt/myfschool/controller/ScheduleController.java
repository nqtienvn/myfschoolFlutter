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
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.ScheduleService;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules", description = "Thời khóa biểu")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/class")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "TKB lớp")
    public ResponseEntity<ApiResponse<ClassScheduleDto>> getClassSchedule(
            @RequestParam Long classId, @RequestParam Long semesterId,
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getClassSchedule(classId, semesterId, date)));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "TKB của học sinh do phụ huynh quản lý")
    public ResponseEntity<ApiResponse<ClassScheduleDto>> getStudentSchedule(
            @PathVariable Long studentId,
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
            scheduleService.getStudentSchedule(studentId, SecurityUtil.getCurrentUserId(), date)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    @Operation(summary = "TKB của học sinh hoặc lịch dạy của giáo viên đang đăng nhập")
    public ResponseEntity<ApiResponse<ClassScheduleDto>> getMySchedule(
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getMySchedule(
            SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole(), date)));
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "TKB giáo viên")
    public ResponseEntity<ApiResponse<ClassScheduleDto>> getTeacherSchedule(
            @RequestParam Long teacherId, @RequestParam Long semesterId,
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getTeacherSchedule(teacherId, semesterId, date)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo tiết TKB")
    public ResponseEntity<ApiResponse<ScheduleDto>> createSchedule(
            @Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo thành công", scheduleService.createSchedule(request)));
    }

    @GetMapping("/timetable/{timetableId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Danh sách tiết trong một phiên bản TKB")
    public ResponseEntity<ApiResponse<List<ScheduleDto>>> getTimetableSlots(@PathVariable Long timetableId) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getTimetableSlots(timetableId)));
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
