package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.controller.entity.SchoolClass;
import vn.edu.fpt.myfschool.controller.entity.Semester;
import vn.edu.fpt.myfschool.controller.entity.Student;
import vn.edu.fpt.myfschool.controller.entity.TuitionBill;
import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.BillStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.repository.*;

import java.util.List;
import java.util.stream.Collectors;

@Service("tuitionBillService")
@RequiredArgsConstructor
@Transactional
public class TuitionBillServiceImpl implements TuitionBillService {

    private final TuitionBillRepository tuitionBillRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final SemesterRepository semesterRepository;
    private final ParentRepository parentRepository;
    private final NotificationService notificationService;

    @Override
    public TuitionBillDto createTuitionBill(TuitionBillRequest request) {
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.studentId()));
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        Semester semester = semesterRepository.findById(request.semesterId())
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", request.semesterId()));

        if (tuitionBillRepository.existsByStudentIdAndSemesterIdAndName(
                request.studentId(), request.semesterId(), request.name())) {
            throw new ConflictException("Khoản học phí đã tồn tại");
        }

        TuitionBill bill = new TuitionBill();
        bill.setStudent(student);
        bill.setCls(cls);
        bill.setSemester(semester);
        bill.setName(request.name());
        bill.setAmount(request.amount());
        bill.setDueDate(request.dueDate());
        bill.setStatus(BillStatus.UNPAID);
        bill = tuitionBillRepository.save(bill);
        return toDto(bill);
    }

    @Transactional(readOnly = true)
    @Override
    public List<TuitionBillDto> getStudentBills(Long studentId) {
        return tuitionBillRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<TuitionBillDto> getClassBills(Long classId, Long semesterId, BillStatus status) {
        if (status != null) {
            return tuitionBillRepository.findByClassIdAndSemesterIdAndStatus(classId, semesterId, status)
                .stream().map(this::toDto).collect(Collectors.toList());
        }
        return tuitionBillRepository.findByClassIdAndSemesterIdOrderByCreatedAtDesc(classId, semesterId)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public void deleteTuitionBill(Long billId) {
        TuitionBill bill = tuitionBillRepository.findById(billId)
            .orElseThrow(() -> new ResourceNotFoundException("TuitionBill", "id", billId));
        if (bill.getStatus() != BillStatus.UNPAID) {
            throw new BadRequestException("Chỉ có thể xóa khoản chưa thanh toán");
        }
        tuitionBillRepository.delete(bill);
    }

    private TuitionBillDto toDto(TuitionBill b) {
        return new TuitionBillDto(b.getId(), b.getStudent().getId(), b.getStudent().getUser().getName(),
            b.getStudent().getStudentCode(), b.getCls().getId(), b.getCls().getName(),
            b.getSemester().getId(), b.getSemester().getName(), b.getName(),
            b.getAmount(), b.getDueDate(), b.getStatus(), b.getPaidAt(), List.of(), b.getCreatedAt());
    }
}
