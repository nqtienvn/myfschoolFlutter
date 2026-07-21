package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.PaymentTransactionDto;
import vn.edu.fpt.myfschool.common.dto.TeacherTuitionStudentDto;
import vn.edu.fpt.myfschool.common.dto.TeacherTuitionSummaryDto;
import vn.edu.fpt.myfschool.common.dto.TuitionBillDto;
import vn.edu.fpt.myfschool.common.dto.TuitionBillRequest;
import vn.edu.fpt.myfschool.common.enums.BillStatus;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.TuitionBill;
import vn.edu.fpt.myfschool.repository.ClassRepository;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.repository.EnrollmentRepository;
import vn.edu.fpt.myfschool.repository.HomeroomAssignmentRepository;
import vn.edu.fpt.myfschool.repository.ParentRepository;
import vn.edu.fpt.myfschool.repository.PaymentTransactionRepository;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
import vn.edu.fpt.myfschool.repository.StudentGuardianRepository;
import vn.edu.fpt.myfschool.repository.StudentRepository;
import vn.edu.fpt.myfschool.repository.TeacherRepository;
import vn.edu.fpt.myfschool.repository.TuitionBillRepository;
import vn.edu.fpt.myfschool.service.TuitionBillService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AcademicYearRepository academicYearRepository;

    @Override
    public TuitionBillDto createTuitionBill(TuitionBillRequest request) {
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.studentId()));
        ClassSemesterContext context = resolveClassSemester(request.classId(), request.semesterId());
        validateStudentEnrollment(student, context.cls());
        validateDueDate(request.dueDate(), context.semester());

        if (tuitionBillRepository.existsByStudentIdAndSemesterIdAndName(
                request.studentId(), request.semesterId(), request.name())) {
            throw new ConflictException("Khoản học phí đã tồn tại");
        }

        TuitionBill bill = new TuitionBill();
        bill.setStudent(student);
        bill.setCls(context.cls());
        bill.setSemester(context.semester());
        bill.setName(request.name());
        bill.setAmount(request.amount());
        bill.setDueDate(request.dueDate());
        bill.setStatus(BillStatus.UNPAID);
        return toDto(tuitionBillRepository.save(bill));
    }

    @Transactional(readOnly = true)
    @Override
    public List<TuitionBillDto> getStudentBills(Long studentId, Long semesterId) {
        Student student = resolveParentStudent(studentId);
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
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<TuitionBillDto> getClassBills(Long classId, Long semesterId, BillStatus status) {
        ClassSemesterContext context = resolveClassSemester(classId, semesterId);
        authorizeTeacherForSelectedSemester(context);
        List<TuitionBill> bills = status == null
            ? tuitionBillRepository.findByClassIdAndSemesterIdOrderByCreatedAtDesc(classId, semesterId)
            : tuitionBillRepository.findByClassIdAndSemesterIdAndStatus(classId, semesterId, status);
        return bills.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<TuitionBillDto> getPaymentRequests(Long academicYearId) {
        if (!academicYearRepository.existsById(academicYearId)) {
            throw new ResourceNotFoundException("AcademicYear", "id", academicYearId);
        }
        return tuitionBillRepository
            .findByAcademicYearIdAndStatus(academicYearId, BillStatus.PROCESSING)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public TeacherTuitionSummaryDto getClassSummary(Long classId, Long semesterId) {
        ClassSemesterContext context = resolveClassSemester(classId, semesterId);
        if (SecurityUtil.getCurrentUserRole() != UserRole.TEACHER) {
            throw new ForbiddenException("Chỉ giáo viên chủ nhiệm mới được xem tổng hợp học phí của lớp");
        }
        authorizeTeacherForSelectedSemester(context);

        List<Student> students = enrollmentRepository.findActiveStudentsByClassAndYear(
            classId, context.cls().getAcademicYear().getId());
        students.sort(Comparator.comparing(
            Student::getStudentCode, Comparator.nullsLast(String::compareTo)));

        Map<Long, List<TuitionBill>> billsByStudent = tuitionBillRepository
            .findByClassIdAndSemesterIdOrderByCreatedAtDesc(classId, semesterId)
            .stream()
            .collect(Collectors.groupingBy(bill -> bill.getStudent().getId()));

        List<TeacherTuitionStudentDto> studentRows = students.stream()
            .map(student -> toTeacherSummaryRow(
                student, billsByStudent.getOrDefault(student.getId(), List.of())))
            .toList();
        int paidStudents = (int) studentRows.stream()
            .filter(row -> "PAID".equals(row.paymentState()))
            .count();
        int studentsWithoutBills = (int) studentRows.stream()
            .filter(row -> "NO_BILLS".equals(row.paymentState()))
            .count();
        int outstandingStudents = studentRows.size() - paidStudents - studentsWithoutBills;

        return new TeacherTuitionSummaryDto(
            context.cls().getId(),
            context.cls().getName(),
            context.semester().getId(),
            context.semester().getName(),
            studentRows.size(),
            paidStudents,
            outstandingStudents,
            studentsWithoutBills,
            studentRows);
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

    private TuitionBillDto toDto(TuitionBill bill) {
        List<PaymentTransactionDto> transactions = paymentTransactionRepository
            .findByTuitionBillIdOrderByCreatedAtDesc(bill.getId())
            .stream()
            .map(transaction -> new PaymentTransactionDto(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getPaymentMethod(),
                transaction.getTransactionRef(),
                transaction.getStatus(),
                transaction.getPaidAt(),
                transaction.getCreatedAt()))
            .toList();
        return new TuitionBillDto(
            bill.getId(),
            bill.getStudent().getId(),
            bill.getStudent().getUser().getName(),
            bill.getStudent().getStudentCode(),
            bill.getCls().getId(),
            bill.getCls().getName(),
            bill.getSemester().getId(),
            bill.getSemester().getName(),
            bill.getFeeTemplate() != null ? bill.getFeeTemplate().getId() : null,
            bill.getFeeTemplate() != null ? bill.getFeeTemplate().getName() : null,
            bill.getName(),
            bill.getAmount(),
            bill.getDueDate(),
            bill.getStatus(),
            bill.getPaidAt(),
            transactions,
            bill.getCreatedAt());
    }

    private TeacherTuitionStudentDto toTeacherSummaryRow(
            Student student, List<TuitionBill> bills) {
        String paymentState;
        if (bills.isEmpty()) {
            paymentState = "NO_BILLS";
        } else if (bills.stream().allMatch(bill -> bill.getStatus() == BillStatus.PAID)) {
            paymentState = "PAID";
        } else if (bills.stream().noneMatch(bill -> bill.getStatus() == BillStatus.UNPAID)) {
            paymentState = "PROCESSING";
        } else {
            paymentState = "UNPAID";
        }
        BigDecimal outstandingAmount = bills.stream()
            .filter(bill -> bill.getStatus() != BillStatus.PAID)
            .map(TuitionBill::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TeacherTuitionStudentDto(
            student.getId(),
            student.getUser().getName(),
            student.getStudentCode(),
            paymentState,
            outstandingAmount,
            bills.stream().map(this::toDto).toList());
    }

    private ClassSemesterContext resolveClassSemester(Long classId, Long semesterId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        if (!cls.getAcademicYear().getId().equals(semester.getAcademicYear().getId())) {
            throw new BadRequestException("Lớp và học kỳ phải thuộc cùng một năm học");
        }
        return new ClassSemesterContext(cls, semester);
    }

    private void validateStudentEnrollment(Student student, SchoolClass cls) {
        boolean enrolledInClass = enrollmentRepository
            .existsByStudentIdAndClsIdAndAcademicYearIdAndStatus(
                student.getId(), cls.getId(), cls.getAcademicYear().getId(), EnrollmentStatus.ACTIVE);
        if (!enrolledInClass) {
            throw new BadRequestException("Học sinh không có ghi danh đang hoạt động trong lớp đã chọn");
        }
    }

    private void validateDueDate(LocalDate dueDate, Semester semester) {
        if (dueDate.isBefore(semester.getStartDate()) || dueDate.isAfter(semester.getEndDate())) {
            throw new BadRequestException("Hạn thanh toán phải nằm trong học kỳ đã chọn");
        }
    }

    private void authorizeTeacherForSelectedSemester(ClassSemesterContext context) {
        if (SecurityUtil.getCurrentUserRole() != UserRole.TEACHER) {
            return;
        }
        var teacher = teacherRepository.findByUserId(SecurityUtil.getCurrentUserId())
            .orElseThrow(() -> new ForbiddenException("Tài khoản không có hồ sơ giáo viên"));
        if (!homeroomAssignmentRepository.existsEffectiveForTeacherClassAndPeriod(
                teacher.getId(),
                context.cls().getId(),
                context.cls().getAcademicYear().getId(),
                context.semester().getStartDate(),
                context.semester().getEndDate())) {
            throw new ForbiddenException(
                "Chỉ giáo viên chủ nhiệm có phân công hiệu lực trong học kỳ mới được xem học phí của lớp");
        }
    }

    private Student resolveParentStudent(Long requestedStudentId) {
        Long userId = SecurityUtil.getCurrentUserId();
        UserRole role = SecurityUtil.getCurrentUserRole();
        if (role != UserRole.PARENT) {
            throw new ForbiddenException("Chỉ phụ huynh được phép xem học phí học sinh");
        }
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

    private record ClassSemesterContext(SchoolClass cls, Semester semester) {}
}
