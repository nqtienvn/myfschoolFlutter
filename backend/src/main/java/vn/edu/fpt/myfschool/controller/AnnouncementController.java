package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.AnnouncementDto;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.CreateAnnouncementRequest;
import vn.edu.fpt.myfschool.common.dto.ReviewAnnouncementRequest;
import vn.edu.fpt.myfschool.common.dto.AdminAnnouncementRequest;
import vn.edu.fpt.myfschool.common.dto.AnnouncementRecipientDto;
import vn.edu.fpt.myfschool.common.dto.AnnouncementReplyRequest;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.AnnouncementService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@Tag(name = "Announcements", description = "Thông báo lớp học")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("/eligible-classes")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> eligibleClasses(@RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(announcementService.getEligibleClasses(
                academicYearId, SecurityUtil.getCurrentUserId())));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Tạo thông báo")
    public ResponseEntity<ApiResponse<AnnouncementDto>> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo thông báo thành công",
                announcementService.createAnnouncement(request.title(), request.body(),
                        request.targetRole(), request.requiresReply() != null ? request.requiresReply() : false,
                        request.academicYearId(), request.classIds(), SecurityUtil.getCurrentUserId())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AnnouncementDto>> update(@PathVariable Long id,
            @Valid @RequestBody CreateAnnouncementRequest request) {
        return ResponseEntity.ok(ApiResponse.success(announcementService.updateAnnouncement(id,
                request.title(), request.body(), request.targetRole(),
                request.requiresReply() != null && request.requiresReply(), request.academicYearId(),
                request.classIds(), SecurityUtil.getCurrentUserId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id, SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa thông báo", null));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AnnouncementDto>>> adminList(
            @RequestParam Long academicYearId, @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(announcementService.getAdminAnnouncements(academicYearId, status)));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementDto>> review(@PathVariable Long id,
            @RequestBody ReviewAnnouncementRequest request) {
        return ResponseEntity.ok(ApiResponse.success(announcementService.review(id, request.approve(),
                request.reason(), SecurityUtil.getCurrentUserId())));
    }

    @PostMapping("/admin/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementDto>> broadcast(@Valid @RequestBody AdminAnnouncementRequest request) {
        return ResponseEntity.ok(ApiResponse.success(announcementService.createAdminAnnouncement(request.title(),
                request.body(), request.academicYearId(), request.recipientScope(), request.targetRole(),
                request.classIds(), request.teacherAudience(), request.subjectId(),
                request.requiresReply() != null && request.requiresReply(), SecurityUtil.getCurrentUserId())));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Thông báo đã gửi")
    public ResponseEntity<ApiResponse<List<AnnouncementDto>>> getMyAnnouncements(
            @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
                announcementService.getMyAnnouncements(SecurityUtil.getCurrentUserId(), academicYearId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Chi tiết thông báo")
    public ResponseEntity<ApiResponse<AnnouncementDto>> getAnnouncementDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                announcementService.getAnnouncementDetail(id, SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    @Operation(summary = "Thông báo cho tôi")
    public ResponseEntity<ApiResponse<List<AnnouncementDto>>> getAnnouncements() {
        UserRole role = SecurityUtil.getCurrentUserRole();
        return ResponseEntity.ok(ApiResponse.success(
                announcementService.getAnnouncements(SecurityUtil.getCurrentUserId(), role)));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    @Operation(summary = "Đánh dấu đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        announcementService.markAsRead(id, SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole());
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đọc", null));
    }

    @PutMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    public ResponseEntity<ApiResponse<Void>> acknowledge(@PathVariable Long id) {
        announcementService.acknowledge(id, SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole());
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận thông báo", null));
    }

    @PutMapping("/{id}/reply")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    public ResponseEntity<ApiResponse<Void>> reply(@PathVariable Long id,
            @Valid @RequestBody AnnouncementReplyRequest request) {
        announcementService.reply(id, request.replyText(), SecurityUtil.getCurrentUserId(),
                SecurityUtil.getCurrentUserRole());
        return ResponseEntity.ok(ApiResponse.success("Đã gửi phản hồi", null));
    }

    @GetMapping("/{id}/recipients")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Page<AnnouncementRecipientDto>>> recipients(
            @PathVariable Long id,
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(announcementService.getRecipients(id, academicYearId,
                classId, role, status, keyword, page, size, SecurityUtil.getCurrentUserId(),
                SecurityUtil.getCurrentUserRole())));
    }

    @GetMapping("/pending-action-count")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    public ResponseEntity<ApiResponse<Long>> pendingActionCount() {
        return ResponseEntity.ok(ApiResponse.success(announcementService.getPendingActionCount(
                SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    @Operation(summary = "Số chưa đọc")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        UserRole role = SecurityUtil.getCurrentUserRole();
        return ResponseEntity.ok(ApiResponse.success(
                announcementService.getUnreadCount(SecurityUtil.getCurrentUserId(), role)));
    }
}
