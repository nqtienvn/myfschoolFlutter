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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.ConversationDto;
import vn.edu.fpt.myfschool.common.dto.CreateConversationRequest;
import vn.edu.fpt.myfschool.common.dto.MessageDto;
import vn.edu.fpt.myfschool.common.dto.SearchResultDto;
import vn.edu.fpt.myfschool.common.dto.SendMessageRequest;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.ConversationService;
import vn.edu.fpt.myfschool.service.MessageService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Hộp thoại tin nhắn")
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
            @RequestParam(required = false) Long afterSeq,
            @RequestParam(defaultValue = "20") int limit) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                messageService.getMessages(id, userId, beforeMessageId, afterSeq, limit)));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Gửi tin nhắn qua REST")
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                conversationService.sendMessage(id, SecurityUtil.getCurrentUserId(), request)));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Đánh dấu đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Long> body) {
        Long lastReadMessageId = body == null ? null : body.get("lastReadMessageId");
        conversationService.markAsRead(id, SecurityUtil.getCurrentUserId(), lastReadMessageId);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đọc", null));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Tổng tin chưa đọc")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount() {
        return ResponseEntity.ok(ApiResponse.success(
                conversationService.getTotalUnreadCount(SecurityUtil.getCurrentUserId())));
    }

    @GetMapping("/search-users")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Tìm kiếm người dùng theo tên hoặc SĐT")
    public ResponseEntity<ApiResponse<List<SearchResultDto>>> searchUsers(
            @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(
                conversationService.searchUsers(SecurityUtil.getCurrentUserId(), keyword)));
    }
}
