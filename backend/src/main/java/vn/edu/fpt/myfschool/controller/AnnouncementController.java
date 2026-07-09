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
import vn.edu.fpt.myfschool.common.dto.AnnouncementDto;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.CreateAnnouncementRequest;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.AnnouncementService;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@Tag(name = "Announcements", description = "Thông báo lớp học")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Tạo thông báo")
    public ResponseEntity<ApiResponse<AnnouncementDto>> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo thông báo thành công",
                announcementService.createAnnouncement(request.title(), request.body(),
                        request.targetRole(), request.requiresReply() != null ? request.requiresReply() : false,
                        request.classIds(), SecurityUtil.getCurrentUserId())));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Thông báo đã gửi")
    public ResponseEntity<ApiResponse<List<AnnouncementDto>>> getMyAnnouncements() {
        return ResponseEntity.ok(ApiResponse.success(
                announcementService.getMyAnnouncements(SecurityUtil.getCurrentUserId())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Chi tiết thông báo")
    public ResponseEntity<ApiResponse<AnnouncementDto>> getAnnouncementDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                announcementService.getAnnouncementDetail(id, SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT')")
    @Operation(summary = "Thông báo cho tôi")
    public ResponseEntity<ApiResponse<List<AnnouncementDto>>> getAnnouncements() {
        UserRole role = SecurityUtil.getCurrentUserRole();
        return ResponseEntity.ok(ApiResponse.success(
                announcementService.getAnnouncements(SecurityUtil.getCurrentUserId(), role)));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT')")
    @Operation(summary = "Đánh dấu đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        announcementService.markAsRead(id, SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole());
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đọc", null));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT')")
    @Operation(summary = "Số chưa đọc")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        UserRole role = SecurityUtil.getCurrentUserRole();
        return ResponseEntity.ok(ApiResponse.success(
                announcementService.getUnreadCount(SecurityUtil.getCurrentUserId(), role)));
    }
}
