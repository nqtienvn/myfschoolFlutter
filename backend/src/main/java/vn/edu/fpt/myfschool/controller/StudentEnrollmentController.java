package vn.edu.fpt.myfschool.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.service.StudentEnrollmentService;

@RestController
@RequestMapping("/api/admin/student-enrollments")
@RequiredArgsConstructor
public class StudentEnrollmentController {
    private final StudentEnrollmentService service;
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StudentEnrollmentResultDto>> create(@Valid @RequestBody CreateStudentEnrollmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo học sinh và xếp lớp thành công", service.create(request)));
    }
}
