package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.TuitionBill;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.BillStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
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
    private final TeacherRepository teacherRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final ParentRepository parentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final EnrollmentRepository enrollmentRepository;

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
    public List<TuitionBillDto> getStudentBills(Long studentId, Long semesterId) {
        Student student = resolveAccessibleStudent(studentId);
        if (semesterId != null) {
            Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
            if (enrollmentRepository.findFirstByStudentIdAndAcademicYearIdOrderByIdDesc(
                    student.getId(), semester.getAcademicYear().getId()).isEmpty()) {
                throw new BadRequestException("Học sinh không thuộc năm học của học kỳ đã chọn");
            }
        }
        return tuitionBillRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())
            .stream()
            .filter(bill -> semesterId == null || bill.getSemester().getId().equals(semesterId))
            .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<TuitionBillDto> getClassBills(Long classId, Long semesterId, BillStatus status) {
        if (SecurityUtil.getCurrentUserRole() == UserRole.TEACHER) {
            var teacher = teacherRepository.findByUserId(SecurityUtil.getCurrentUserId())
                .orElseThrow(() -> new ForbiddenException("Tài khoản không có hồ sơ giáo viên"));
            SchoolClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
            if (!homeroomAssignmentRepository.existsByTeacherIdAndClsIdAndAcademicYearId(
                    teacher.getId(), classId, cls.getAcademicYear().getId())) {
                throw new ForbiddenException("Chỉ giáo viên chủ nhiệm mới được xem và quản lý học phí của lớp");
            }
        }
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
            b.getSemester().getId(), b.getSemester().getName(),
            b.getFeeTemplate() != null ? b.getFeeTemplate().getId() : null,
            b.getFeeTemplate() != null ? b.getFeeTemplate().getName() : null,
            b.getName(), b.getAmount(), b.getDueDate(), b.getStatus(), b.getPaidAt(), List.of(), b.getCreatedAt());
    }

    private Student resolveAccessibleStudent(Long requestedStudentId) {
        Long userId = SecurityUtil.getCurrentUserId();
        UserRole role = SecurityUtil.getCurrentUserRole();
        if (role == UserRole.STUDENT) {
            Student own = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", userId));
            if (requestedStudentId != null && !requestedStudentId.equals(own.getId())) {
                throw new ForbiddenException("Học sinh chỉ được xem học phí của chính mình");
            }
            return own;
        }
        if (role == UserRole.PARENT) {
            if (requestedStudentId == null) {
                throw new BadRequestException("Phụ huynh phải chọn học sinh cần xem học phí");
            }
            Parent parent = parentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", "userId", userId));
            if (!studentGuardianRepository.existsByStudentIdAndGuardianId(
                    requestedStudentId, parent.getId())) {
                throw new ForbiddenException("Phụ huynh không có quyền xem học phí của học sinh này");
            }
            return studentRepository.findById(requestedStudentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", requestedStudentId));
        }
        throw new ForbiddenException("Vai trò không được phép xem học phí học sinh");
    }
}
