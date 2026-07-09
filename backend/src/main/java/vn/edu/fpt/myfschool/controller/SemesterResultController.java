package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.ClassRankingDto;
import vn.edu.fpt.myfschool.common.dto.SemesterResultDto;
import vn.edu.fpt.myfschool.service.SemesterResultService;

@RestController
@RequestMapping("/api/semester-results")
@RequiredArgsConstructor
@Tag(name = "Semester Results", description = "Tổng kết học kỳ")
public class SemesterResultController {

    private final SemesterResultService semesterResultService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Xem tổng kết học kỳ")
    public ResponseEntity<ApiResponse<SemesterResultDto>> getSemesterResult(
            @RequestParam Long studentId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(semesterResultService.getStudentSemesterResult(studentId, semesterId)));
    }

    @GetMapping("/ranking")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Xếp hạng lớp")
    public ResponseEntity<ApiResponse<ClassRankingDto>> getClassRanking(
            @RequestParam Long classId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(semesterResultService.getClassRanking(classId, semesterId)));
    }
}
