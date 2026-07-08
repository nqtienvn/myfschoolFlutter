package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.InitializeAcademicYearResponse;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.FeeTemplate;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.TeachingAssignment;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.repository.ClassRepository;
import vn.edu.fpt.myfschool.repository.FeeTemplateRepository;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
import vn.edu.fpt.myfschool.repository.TeachingAssignmentRepository;
import vn.edu.fpt.myfschool.service.AcademicYearInitializationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AcademicYearInitializationServiceImpl implements AcademicYearInitializationService {

    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final FeeTemplateRepository feeTemplateRepository;
    private final SemesterRepository semesterRepository;

    @Override
    public InitializeAcademicYearResponse initialize(Long newAcademicYearId, Long fromAcademicYearId) {
        AcademicYear newYear = academicYearRepository.findById(newAcademicYearId)
            .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", newAcademicYearId));
        AcademicYear fromYear = academicYearRepository.findById(fromAcademicYearId)
            .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", fromAcademicYearId));

        List<String> warnings = new ArrayList<>();

        // Map Semesters from previous year to new year by order (or name fallback)
        List<Semester> fromSemesters = semesterRepository.findByAcademicYearIdOrderByOrderAsc(fromYear.getId());
        List<Semester> newSemesters = semesterRepository.findByAcademicYearIdOrderByOrderAsc(newYear.getId());

        Map<Long, Semester> semesterMapping = new HashMap<>();
        for (Semester fromSem : fromSemesters) {
            Semester match = newSemesters.stream()
                .filter(ns -> ns.getOrder().equals(fromSem.getOrder()))
                .findFirst()
                .orElseGet(() -> newSemesters.stream()
                    .filter(ns -> ns.getName().equalsIgnoreCase(fromSem.getName()))
                    .findFirst()
                    .orElse(null));
            if (match != null) {
                semesterMapping.put(fromSem.getId(), match);
            } else {
                warnings.add("Không tìm thấy học kỳ tương ứng cho " + fromSem.getName() + " (Order: " + fromSem.getOrder() + ") ở năm học mới.");
            }
        }

        // 1. Copy classes
        List<SchoolClass> fromClasses = classRepository.findByAcademicYearId(fromYear.getId());
        int classesCreated = 0;
        Map<String, SchoolClass> classMap = new HashMap<>(); // name -> new class

        for (SchoolClass old : fromClasses) {
            if (classRepository.existsByNameAndAcademicYearId(old.getName(), newYear.getId())) {
                warnings.add("Lớp " + old.getName() + " đã tồn tại ở năm học mới, bỏ qua.");
                // Fetch the existing class to map it for assignments/templates copy
                classRepository.findByNameAndAcademicYearId(old.getName(), newYear.getId())
                    .ifPresent(c -> classMap.put(old.getName(), c));
                continue;
            }

            SchoolClass newCls = new SchoolClass();
            newCls.setName(old.getName());
            newCls.setGradeLevel(old.getGradeLevel());
            newCls.setAcademicYear(newYear);
            newCls.setSchoolName(old.getSchoolName());
            SchoolClass savedCls = classRepository.save(newCls);
            classMap.put(old.getName(), savedCls);
            classesCreated++;
        }

        // 2. Copy teaching assignments (template only - teacher set to null)
        int assignmentsCopied = 0;
        List<TeachingAssignment> fromAssignments = teachingAssignmentRepository.findByAcademicYearId(fromYear.getId());

        for (TeachingAssignment old : fromAssignments) {
            SchoolClass newCls = classMap.get(old.getCls().getName());
            if (newCls == null) continue;

            Semester targetSemester = semesterMapping.get(old.getSemester().getId());
            if (targetSemester == null) continue;

            // Avoid duplicating identical assignment templates
            if (teachingAssignmentRepository.existsByClsIdAndSubjectIdAndSemesterIdAndEffectiveFrom(
                    newCls.getId(), old.getSubject().getId(), targetSemester.getId(), newYear.getStartDate())) {
                continue;
            }

            TeachingAssignment ta = new TeachingAssignment();
            ta.setCls(newCls);
            ta.setSubject(old.getSubject());
            ta.setTeacher(null); // Keep teacher empty for admin to assign later
            ta.setSemester(targetSemester);
            ta.setEffectiveFrom(newYear.getStartDate());
            ta.setStatus(AssignmentStatus.INACTIVE); // inactive by default until teacher is assigned
            teachingAssignmentRepository.save(ta);
            assignmentsCopied++;
        }

        // 3. Copy fee templates
        int templatesCopied = 0;
        List<FeeTemplate> fromTemplates = feeTemplateRepository.findByAcademicYearId(fromYear.getId());

        for (FeeTemplate old : fromTemplates) {
            SchoolClass newCls = classMap.get(old.getCls().getName());
            if (newCls == null) continue;

            Semester targetSemester = semesterMapping.get(old.getSemester().getId());
            if (targetSemester == null) continue;

            if (feeTemplateRepository.existsByFeeCategoryIdAndClsIdAndSemesterId(
                    old.getFeeCategory().getId(), newCls.getId(), targetSemester.getId())) {
                continue;
            }

            FeeTemplate ft = new FeeTemplate();
            ft.setFeeCategory(old.getFeeCategory());
            ft.setCls(newCls);
            ft.setSemester(targetSemester);
            ft.setName(old.getName());
            ft.setAmount(old.getAmount());
            // Due date is set relative to new year start date or copy old logic if needed, but let's shift it or default to newYear.getStartDate() + 30 days
            ft.setDueDate(newYear.getStartDate().plusDays(30));
            feeTemplateRepository.save(ft);
            templatesCopied++;
        }

        return new InitializeAcademicYearResponse(
            newYear.getId(), classesCreated, assignmentsCopied, templatesCopied, warnings);
    }
}
