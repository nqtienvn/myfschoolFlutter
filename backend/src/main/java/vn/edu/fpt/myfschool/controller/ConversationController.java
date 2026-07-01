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
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.ConversationService;
import vn.edu.fpt.myfschool.service.MessageService;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Hộp thoại tin nhắn")
@SecurityRequirement(name = "Bearer Authentication")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Danh sách hộp thoại")
    public ResponseEntity<ApiResponse<List<ConversationDto>>> getConversations() {
        return ResponseEntity.ok(ApiResponse.success(
            conversationService.getConversations(SecurityUtil.getCurrentUserId())));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Tạo/tìm hộp thoại")
    public ResponseEntity<ApiResponse<ConversationDto>> createConversation(
            @Valid @RequestBody CreateConversationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            conversationService.createOrFindConversation(SecurityUtil.getCurrentUserId(), request.otherUserId())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Chi tiết hộp thoại")
    public ResponseEntity<ApiResponse<List<MessageDto>>> getMessages(
            @PathVariable Long id,
            @RequestParam(required = false) Long beforeMessageId,
            @RequestParam(defaultValue = "20") int limit) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(messageService.getMessages(id, userId, beforeMessageId, limit)));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Đánh dấu đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        conversationService.markAsRead(id, SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đọc", null));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Tổng tin chưa đọc")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount() {
        return ResponseEntity.ok(ApiResponse.success(
            conversationService.getTotalUnreadCount(SecurityUtil.getCurrentUserId())));
    }
}
