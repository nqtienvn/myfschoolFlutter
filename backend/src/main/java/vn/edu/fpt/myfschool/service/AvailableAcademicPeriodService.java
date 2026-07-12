package vn.edu.fpt.myfschool.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.AvailableAcademicYearDto;
import vn.edu.fpt.myfschool.common.dto.SemesterDto;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.mapper.SemesterMapper;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.repository.EnrollmentRepository;
import vn.edu.fpt.myfschool.repository.HomeroomAssignmentRepository;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
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
    private final SemesterMapper semesterMapper;

    @Transactional(readOnly = true)
    public List<AvailableAcademicYearDto> getAvailablePeriods() {
        Long userId = SecurityUtil.getCurrentUserId();
        UserRole role = SecurityUtil.getCurrentUserRole();
        Set<Long> yearIds = new LinkedHashSet<>();
        if (role == UserRole.STUDENT) {
            yearIds.addAll(enrollmentRepository.findAcademicYearIdsByStudentUserId(userId));
        } else if (role == UserRole.PARENT) {
            yearIds.addAll(enrollmentRepository.findAcademicYearIdsByParentUserId(userId));
        } else if (role == UserRole.TEACHER) {
            yearIds.addAll(teachingAssignmentRepository.findAcademicYearIdsByTeacherUserId(userId));
            yearIds.addAll(homeroomAssignmentRepository.findAcademicYearIdsByTeacherUserId(userId));
        }
        return academicYearRepository.findAllById(yearIds).stream()
            .sorted(Comparator.comparing(AcademicYear::getStartDate).reversed())
            .map(year -> new AvailableAcademicYearDto(
                year.getId(),
                year.getName(),
                semesterRepository.findByAcademicYearIdOrderByOrderAsc(year.getId()).stream()
                    .map(semesterMapper::toDto)
                    .toList()))
            .toList();
    }
}
