package vn.edu.fpt.myfschool.controller;
import lombok.RequiredArgsConstructor; import org.springframework.http.ResponseEntity; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.*; import vn.edu.fpt.myfschool.service.TranscriptService;
@RestController @RequestMapping("/api/transcripts") @RequiredArgsConstructor public class TranscriptController { private final TranscriptService service;
 @GetMapping("/students/{studentId}") @PreAuthorize("hasAnyRole('PARENT','STUDENT')") public ResponseEntity<ApiResponse<StudentTranscriptDto>> get(@PathVariable Long studentId,@RequestParam Long academicYearId,@RequestParam Long semesterId){return ResponseEntity.ok(ApiResponse.success(service.get(studentId,academicYearId,semesterId)));}}
