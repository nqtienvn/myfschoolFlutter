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
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.TuitionBill;
import vn.edu.fpt.myfschool.repository.ParentRepository;
import vn.edu.fpt.myfschool.repository.PaymentTransactionRepository;
import vn.edu.fpt.myfschool.repository.StudentGuardianRepository;
import vn.edu.fpt.myfschool.repository.StudentRepository;
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
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final StudentGuardianRepository studentGuardianRepository;

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

    private TuitionBill getBillForUpdate(Long billId) {
        return tuitionBillRepository.findByIdForUpdate(billId)
            .orElseThrow(() -> new ResourceNotFoundException("TuitionBill", "id", billId));
    }

    private void authorizeBillOwner(TuitionBill bill) {
        Long userId = SecurityUtil.getCurrentUserId();
        UserRole role = SecurityUtil.getCurrentUserRole();
        if (role == UserRole.STUDENT) {
            Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản không có hồ sơ học sinh"));
            if (!student.getId().equals(bill.getStudent().getId())) {
                throw new ForbiddenException("Học sinh chỉ được xác nhận học phí của chính mình");
            }
            return;
        }
        if (role == UserRole.PARENT) {
            Parent parent = parentRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản không có hồ sơ phụ huynh"));
            if (!studentGuardianRepository.existsByStudentIdAndGuardianId(
                    bill.getStudent().getId(), parent.getId())) {
                throw new ForbiddenException(
                    "Phụ huynh không có quyền xác nhận học phí của học sinh này");
            }
            return;
        }
        throw new ForbiddenException("Vai trò không được phép xác nhận chuyển khoản học phí");
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
