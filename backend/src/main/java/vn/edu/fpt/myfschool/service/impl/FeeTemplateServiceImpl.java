package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.BillStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.controller.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.FeeTemplateService;

import java.util.List;

@Service("feeTemplateService")
@RequiredArgsConstructor
@Transactional
public class FeeTemplateServiceImpl implements FeeTemplateService {

    private final FeeTemplateRepository feeTemplateRepository;
    private final FeeCategoryRepository feeCategoryRepository;
    private final ClassRepository classRepository;
    private final SemesterRepository semesterRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TuitionBillRepository tuitionBillRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FeeTemplateDto> listByClass(Long classId, Long semesterId) {
        return feeTemplateRepository.findByClsIdAndSemesterId(classId, semesterId)
            .stream().map(this::toDto).toList();
    }

    @Override
    public FeeTemplateDto create(CreateFeeTemplateRequest request) {
        FeeCategory fc = feeCategoryRepository.findById(request.feeCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("FeeCategory", "id", request.feeCategoryId()));
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        Semester semester = semesterRepository.findById(request.semesterId())
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", request.semesterId()));
        if (!cls.getAcademicYear().getId().equals(semester.getAcademicYear().getId())) {
            throw new BadRequestException("Lop va hoc ky phai cung nam hoc");
        }

        if (feeTemplateRepository.existsByFeeCategoryIdAndClsIdAndSemesterId(
                request.feeCategoryId(), request.classId(), request.semesterId())) {
            throw new ConflictException("Template da ton tai cho lop va hoc ky nay");
        }

        FeeTemplate ft = new FeeTemplate();
        ft.setFeeCategory(fc);
        ft.setCls(cls);
        ft.setSemester(semester);
        ft.setName(request.name());
        ft.setAmount(request.amount());
        ft.setDueDate(request.dueDate());
        ft = feeTemplateRepository.save(ft);

        List<Student> students = enrollmentRepository.findActiveStudentsByClassAndYear(
            request.classId(), cls.getAcademicYear().getId());
        return new FeeTemplateDto(ft.getId(), fc.getId(), fc.getName(),
            cls.getId(), cls.getName(), semester.getId(), semester.getName(),
            ft.getName(), ft.getAmount(), ft.getDueDate(), students.size());
    }

    @Override
    public GenerateBillResultDto generateBills(Long feeTemplateId) {
        FeeTemplate ft = feeTemplateRepository.findById(feeTemplateId)
            .orElseThrow(() -> new ResourceNotFoundException("FeeTemplate", "id", feeTemplateId));

        List<Student> students = enrollmentRepository.findActiveStudentsByClassAndYear(
            ft.getCls().getId(), ft.getCls().getAcademicYear().getId());

        int created = 0, skipped = 0;
        for (Student s : students) {
            if (tuitionBillRepository.existsByStudentIdAndSemesterIdAndFeeTemplateId(
                    s.getId(), ft.getSemester().getId(), ft.getId())) {
                skipped++;
                continue;
            }
            TuitionBill bill = new TuitionBill();
            bill.setStudent(s);
            bill.setCls(ft.getCls());
            bill.setSemester(ft.getSemester());
            bill.setFeeTemplate(ft);
            bill.setName(ft.getName());
            bill.setAmount(ft.getAmount());
            bill.setDueDate(ft.getDueDate());
            bill.setStatus(BillStatus.UNPAID);
            tuitionBillRepository.save(bill);
            created++;
        }

        return new GenerateBillResultDto(ft.getId(), students.size(), created, skipped);
    }

    @Override
    public void delete(Long id) {
        FeeTemplate ft = feeTemplateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FeeTemplate", "id", id));
        if (tuitionBillRepository.existsByFeeTemplateId(id)) {
            throw new ConflictException("Khong the xoa template da sinh hoa don");
        }
        feeTemplateRepository.delete(ft);
    }

    private FeeTemplateDto toDto(FeeTemplate ft) {
        List<Student> students = enrollmentRepository.findActiveStudentsByClassAndYear(
            ft.getCls().getId(), ft.getCls().getAcademicYear().getId());
        return new FeeTemplateDto(ft.getId(),
            ft.getFeeCategory().getId(), ft.getFeeCategory().getName(),
            ft.getCls().getId(), ft.getCls().getName(),
            ft.getSemester().getId(), ft.getSemester().getName(),
            ft.getName(), ft.getAmount(), ft.getDueDate(), students.size());
    }
}