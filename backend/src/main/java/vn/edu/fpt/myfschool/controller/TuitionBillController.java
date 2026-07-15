package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.PaymentTransactionDto;
import vn.edu.fpt.myfschool.common.dto.TuitionBillDto;
import vn.edu.fpt.myfschool.common.dto.TuitionBillRequest;
import vn.edu.fpt.myfschool.common.dto.TeacherTuitionSummaryDto;
import vn.edu.fpt.myfschool.common.enums.BillStatus;
import vn.edu.fpt.myfschool.service.PaymentTransactionService;
import vn.edu.fpt.myfschool.service.TuitionBillService;

import java.util.List;

@RestController
@RequestMapping("/api/tuition")
@RequiredArgsConstructor
@Tag(name = "Tuition", description = "Học phí & Thanh toán")
public class TuitionBillController {

    private final TuitionBillService tuitionBillService;
    private final PaymentTransactionService paymentTransactionService;

    @PostMapping("/bills")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo khoản HP")
    public ResponseEntity<ApiResponse<TuitionBillDto>> createBill(
            @Valid @RequestBody TuitionBillRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo khoản thành công", tuitionBillService.createTuitionBill(request)));
    }

    @GetMapping("/bills/class")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "HP theo lớp")
    public ResponseEntity<ApiResponse<List<TuitionBillDto>>> getClassBills(
            @RequestParam Long classId, @RequestParam Long semesterId,
            @RequestParam(required = false) BillStatus status) {
        return ResponseEntity.ok(ApiResponse.success(tuitionBillService.getClassBills(classId, semesterId, status)));
    }

    @GetMapping("/bills/class-summary")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Tổng hợp học phí lớp chủ nhiệm theo học kỳ")
    public ResponseEntity<ApiResponse<TeacherTuitionSummaryDto>> getClassSummary(
            @RequestParam Long classId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(
            tuitionBillService.getClassSummary(classId, semesterId)));
    }

    @GetMapping("/bills/student")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    @Operation(summary = "Học phí của học sinh theo học kỳ")
    public ResponseEntity<ApiResponse<List<TuitionBillDto>>> getStudentBills(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(
            tuitionBillService.getStudentBills(studentId, semesterId)));
    }

    @GetMapping("/payment-requests")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách xác nhận chuyển khoản đang chờ đối soát theo năm học")
    public ResponseEntity<ApiResponse<List<TuitionBillDto>>> getPaymentRequests(
            @RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
            tuitionBillService.getPaymentRequests(academicYearId)));
    }

    @DeleteMapping("/bills/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa khoản HP")
    public ResponseEntity<ApiResponse<Void>> deleteBill(@PathVariable Long id) {
        tuitionBillService.deleteTuitionBill(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công", null));
    }

    @PostMapping("/bills/{id}/simulate-pay")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mô phỏng thanh toán")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> simulatePayment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Thanh toán thành công", paymentTransactionService.simulatePayment(id)));
    }

    @PostMapping("/bills/{id}/payment-request")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    @Operation(summary = "Ghi nhận xác nhận chuyển khoản và chờ nhà trường đối soát")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> requestBankTransfer(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            "Đã ghi nhận xác nhận chuyển khoản",
            paymentTransactionService.requestBankTransfer(id)));
    }

    @PostMapping("/bills/{id}/confirm-payment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xác nhận đã nhận tiền chuyển khoản")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> confirmBankTransfer(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            "Đã xác nhận thanh toán",
            paymentTransactionService.confirmBankTransfer(id)));
    }

    @PostMapping("/bills/{id}/reject-payment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Từ chối xác nhận khi không tìm thấy giao dịch")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> rejectBankTransfer(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            "Đã trả khoản học phí về trạng thái chưa đóng",
            paymentTransactionService.rejectBankTransfer(id)));
    }
}
