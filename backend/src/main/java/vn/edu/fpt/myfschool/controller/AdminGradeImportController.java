package vn.edu.fpt.myfschool.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportBatchDto;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportContextDto;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportItemDto;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportResultDto;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportTableDto;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.UpdateAdminGradeImportRowRequest;
import vn.edu.fpt.myfschool.service.AdminGradeImportService;

@RestController
@RequestMapping("/api/admin-grade-imports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminGradeImportController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final AdminGradeImportService service;

    @GetMapping("/context")
    public ResponseEntity<ApiResponse<AdminGradeImportContextDto>> context() {
        return ResponseEntity.ok(ApiResponse.success(service.getContext()));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<AdminGradeImportItemDto>>> items() {
        return ResponseEntity.ok(ApiResponse.success(service.getItems()));
    }

    @GetMapping("/template/{itemCode}")
    public ResponseEntity<byte[]> template(@PathVariable String itemCode) {
        byte[] bytes = service.createTemplate(itemCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(XLSX);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("template-import-" + itemCode + ".xlsx", StandardCharsets.UTF_8).build());
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminGradeImportResultDto>> importFile(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Đã lưu dữ liệu điểm nháp", service.importFile(file)));
    }

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<List<AdminGradeImportBatchDto>>> batches() {
        return ResponseEntity.ok(ApiResponse.success(service.getBatches()));
    }

    @GetMapping("/batches/{batchId}")
    public ResponseEntity<ApiResponse<AdminGradeImportTableDto>> batch(@PathVariable Long batchId,
                                                                         @RequestParam(required = false) Long classId) {
        return ResponseEntity.ok(ApiResponse.success(service.getBatch(batchId, classId)));
    }

    @PutMapping("/batches/{batchId}/students/{studentId}")
    public ResponseEntity<ApiResponse<AdminGradeImportTableDto>> updateRow(@PathVariable Long batchId,
                                                                             @PathVariable Long studentId,
                                                                             @Valid @RequestBody UpdateAdminGradeImportRowRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã lưu thay đổi điểm", service.updateRow(batchId, studentId, request)));
    }
}
