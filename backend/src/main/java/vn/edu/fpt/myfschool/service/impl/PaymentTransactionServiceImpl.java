package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.PaymentTransactionDto;
import vn.edu.fpt.myfschool.common.enums.BillStatus;
import vn.edu.fpt.myfschool.common.enums.PaymentStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.PaymentTransaction;
import vn.edu.fpt.myfschool.entity.TuitionBill;
import vn.edu.fpt.myfschool.repository.ParentRepository;
import vn.edu.fpt.myfschool.repository.PaymentConfigurationRepository;
import vn.edu.fpt.myfschool.repository.PaymentTransactionRepository;
import vn.edu.fpt.myfschool.repository.StudentGuardianRepository;
import vn.edu.fpt.myfschool.repository.TuitionBillRepository;
import vn.edu.fpt.myfschool.service.PaymentTransactionService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service("paymentTransactionService")
@RequiredArgsConstructor
@Transactional
public class PaymentTransactionServiceImpl implements PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final TuitionBillRepository tuitionBillRepository;
    private final ParentRepository parentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final PaymentConfigurationRepository paymentConfigurationRepository;

    @Override
    public PaymentTransactionDto requestBankTransfer(Long billId) {
        TuitionBill bill = getBillForUpdate(billId);
        authorizeBillOwner(bill);
        if (bill.getStatus() == BillStatus.PAID) {
            throw new BadRequestException("Khoản học phí đã được thanh toán");
        }
        if (bill.getStatus() == BillStatus.PROCESSING) {
            throw new ConflictException("Khoản học phí đang chờ nhà trường đối soát");
        }

        Long academicYearId = bill.getSemester().getAcademicYear().getId();
        if (paymentConfigurationRepository
                .findByAcademicYearIdAndEnabledTrue(academicYearId)
                .isEmpty()) {
            throw new ConflictException(
                "Nhà trường chưa kích hoạt tài khoản nhận chuyển khoản cho năm học này");
        }

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTuitionBill(bill);
        transaction.setAmount(bill.getAmount());
        transaction.setPaymentMethod("BANK_TRANSFER_CONFIRMATION");
        transaction.setTransactionRef("MFS-" + UUID.randomUUID());
        transaction.setStatus(PaymentStatus.PENDING);
        transaction = paymentTransactionRepository.save(transaction);

        bill.setStatus(BillStatus.PROCESSING);
        bill.setPaidAt(null);
        tuitionBillRepository.save(bill);
        return toDto(transaction);
    }

    @Override
    public PaymentTransactionDto simulatePayment(Long billId) {
        TuitionBill bill = getBillForUpdate(billId);
        if (bill.getStatus() == BillStatus.PAID) {
            throw new BadRequestException("Khoản học phí đã được thanh toán");
        }

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTuitionBill(bill);
        transaction.setAmount(bill.getAmount());
        transaction.setPaymentMethod("SIMULATED");
        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setPaidAt(LocalDateTime.now());
        transaction = paymentTransactionRepository.save(transaction);

        bill.setStatus(BillStatus.PAID);
        bill.setPaidAt(LocalDateTime.now());
        tuitionBillRepository.save(bill);
        return toDto(transaction);
    }

    @Override
    public PaymentTransactionDto confirmBankTransfer(Long billId) {
        TuitionBill bill = getBillForUpdate(billId);
        PaymentTransaction transaction = requirePendingBankTransfer(bill);
        LocalDateTime paidAt = LocalDateTime.now();
        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setPaidAt(paidAt);
        paymentTransactionRepository.save(transaction);

        bill.setStatus(BillStatus.PAID);
        bill.setPaidAt(paidAt);
        tuitionBillRepository.save(bill);
        return toDto(transaction);
    }

    @Override
    public PaymentTransactionDto rejectBankTransfer(Long billId) {
        TuitionBill bill = getBillForUpdate(billId);
        PaymentTransaction transaction = requirePendingBankTransfer(bill);
        transaction.setStatus(PaymentStatus.FAILED);
        transaction.setPaidAt(null);
        paymentTransactionRepository.save(transaction);

        bill.setStatus(BillStatus.UNPAID);
        bill.setPaidAt(null);
        tuitionBillRepository.save(bill);
        return toDto(transaction);
    }

    private TuitionBill getBillForUpdate(Long billId) {
        return tuitionBillRepository.findByIdForUpdate(billId)
            .orElseThrow(() -> new ResourceNotFoundException("TuitionBill", "id", billId));
    }

    private PaymentTransaction requirePendingBankTransfer(TuitionBill bill) {
        if (bill.getStatus() != BillStatus.PROCESSING) {
            throw new ConflictException("Khoản học phí không ở trạng thái chờ đối soát");
        }
        return paymentTransactionRepository
            .findFirstByTuitionBillIdAndStatusOrderByCreatedAtDesc(
                bill.getId(), PaymentStatus.PENDING)
            .orElseThrow(() -> new ConflictException(
                "Không tìm thấy xác nhận chuyển khoản đang chờ đối soát"));
    }

    private void authorizeBillOwner(TuitionBill bill) {
        Long userId = SecurityUtil.getCurrentUserId();
        UserRole role = SecurityUtil.getCurrentUserRole();
        if (role != UserRole.PARENT) {
            throw new ForbiddenException("Chỉ phụ huynh được phép xác nhận chuyển khoản học phí");
        }
        Parent parent = parentRepository.findByUserId(userId)
            .orElseThrow(() -> new ForbiddenException("Tài khoản không có hồ sơ phụ huynh"));
        if (!studentGuardianRepository.existsByStudentIdAndGuardianId(
                bill.getStudent().getId(), parent.getId())) {
            throw new ForbiddenException(
                "Phụ huynh không có quyền xác nhận học phí của học sinh này");
        }
    }

    private PaymentTransactionDto toDto(PaymentTransaction transaction) {
        return new PaymentTransactionDto(
            transaction.getId(),
            transaction.getAmount(),
            transaction.getPaymentMethod(),
            transaction.getTransactionRef(),
            transaction.getStatus(),
            transaction.getPaidAt(),
            transaction.getCreatedAt());
    }
}
