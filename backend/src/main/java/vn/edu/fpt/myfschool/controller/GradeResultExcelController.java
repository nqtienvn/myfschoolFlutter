package vn.edu.fpt.myfschool.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.GradeImportResultDto;
import vn.edu.fpt.myfschool.service.GradeResultExcelService;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/result-files")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class GradeResultExcelController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final GradeResultExcelService excelService;

    @GetMapping("/template")
    public ResponseEntity<byte[]> template(@RequestParam Long academicYearId,
                                           @RequestParam Long semesterId,
                                           @RequestParam Long classId,
                                           @RequestParam Long subjectId) {
        return workbook(excelService.createTemplate(academicYearId, semesterId, classId, subjectId),
                "template-diem-admin.xlsx");
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GradeImportResultDto>> importScores(
            @RequestParam Long academicYearId,
            @RequestParam Long semesterId,
            @RequestParam Long classId,
            @RequestParam Long subjectId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Import điểm thành công",
                excelService.importScores(academicYearId, semesterId, classId, subjectId, file)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam Long academicYearId,
                                         @RequestParam(required = false) Long semesterId,
                                         @RequestParam(required = false) Long classId) {
        return workbook(excelService.exportResults(academicYearId, semesterId, classId),
                "ket-qua-hoc-tap.xlsx");
    }

    private ResponseEntity<byte[]> workbook(byte[] bytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(XLSX);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8).build());
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
