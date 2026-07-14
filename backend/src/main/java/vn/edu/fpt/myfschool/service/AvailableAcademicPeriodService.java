package vn.edu.fpt.myfschool.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.AvailableAcademicYearDto;
import vn.edu.fpt.myfschool.common.dto.SemesterDto;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.SemesterStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.mapper.SemesterMapper;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.repository.EnrollmentRepository;
import vn.edu.fpt.myfschool.repository.HomeroomAssignmentRepository;
import vn.edu.fpt.myfschool.repository.ParentRepository;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
import vn.edu.fpt.myfschool.repository.StudentGuardianRepository;
import vn.edu.fpt.myfschool.repository.TeachingAssignmentRepository;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AvailableAcademicPeriodService {
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final ParentRepository parentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final SemesterMapper semesterMapper;

    @Transactional(readOnly = true)
    public List<AvailableAcademicYearDto> getAvailablePeriods(Long selectedStudentId) {
        Long userId = SecurityUtil.getCurrentUserId();
        UserRole role = SecurityUtil.getCurrentUserRole();
        Set<Long> yearIds = new LinkedHashSet<>();
        if (role == UserRole.STUDENT) {
            yearIds.addAll(enrollmentRepository.findAcademicYearIdsByStudentUserId(userId));
        } else if (role == UserRole.PARENT) {
            if (selectedStudentId == null) {
                yearIds.addAll(enrollmentRepository.findAcademicYearIdsByParentUserId(userId));
            } else {
                Parent parent = parentRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent", "userId", userId));
                if (!studentGuardianRepository.existsByStudentIdAndGuardianId(
                        selectedStudentId, parent.getId())) {
                    throw new ForbiddenException("Phụ huynh không có quyền xem dữ liệu của học sinh này");
                }
                yearIds.addAll(enrollmentRepository.findAcademicYearIdsByStudentId(selectedStudentId));
            }
        } else if (role == UserRole.TEACHER) {
            yearIds.addAll(teachingAssignmentRepository.findAcademicYearIdsByTeacherUserId(userId));
            yearIds.addAll(homeroomAssignmentRepository.findAcademicYearIdsByTeacherUserId(userId));
        }
        return academicYearRepository.findAllById(yearIds).stream()
            .filter(year -> year.getStatus() != AcademicYearStatus.DRAFT)
            .sorted(Comparator.comparing(AcademicYear::getStartDate).reversed())
            .map(year -> new AvailableAcademicYearDto(
                year.getId(),
                year.getName(),
                year.getStatus(),
                semesterRepository.findByAcademicYearIdOrderByOrderAsc(year.getId()).stream()
                    .filter(semester -> !Boolean.TRUE.equals(semester.getDeleted()))
                    .filter(semester -> Boolean.TRUE.equals(semester.getIsCurrent())
                        || semester.getStatus() != SemesterStatus.NOT_STARTED)
                    .map(semesterMapper::toDto)
                    .toList()))
            .filter(year -> !year.semesters().isEmpty())
            .toList();
    }
}
