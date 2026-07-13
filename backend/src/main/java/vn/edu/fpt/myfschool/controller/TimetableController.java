package vn.edu.fpt.myfschool.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.service.TimetableService;
import vn.edu.fpt.myfschool.service.TimetableGenerationService;

import java.util.List;

@RestController
@RequestMapping("/api/timetables")
@RequiredArgsConstructor
public class TimetableController {
    private final TimetableService timetableService;
    private final TimetableGenerationService timetableGenerationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<TimetableDto>>> list(
            @RequestParam Long classId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.list(classId, semesterId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<TimetableDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TimetableDto>> create(@Valid @RequestBody CreateTimetableRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã tạo thời khóa biểu nháp", timetableService.createDraft(request)));
    }

    @PostMapping("/auto-generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AutoGenerateTimetableResult>> autoGenerate(
            @Valid @RequestBody AutoGenerateTimetableRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Đã tự động tạo thời khóa biểu nháp",
            timetableGenerationService.generate(request)));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TimetableDto>> publish(
            @PathVariable Long id, @Valid @RequestBody PublishTimetableRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã phát hành thời khóa biểu", timetableService.publish(id, request.effectiveFrom())));
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TimetableDto>> schedule(
            @PathVariable Long id, @Valid @RequestBody PublishTimetableRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Đã hẹn ngày phát hành thời khóa biểu",
            timetableService.schedulePublish(id, request.effectiveFrom())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        timetableService.deleteDraft(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa thời khóa biểu nháp", null));
    }
}
