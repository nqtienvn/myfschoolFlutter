package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.PromoteResponse;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.Enrollment;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.repository.ClassRepository;
import vn.edu.fpt.myfschool.repository.EnrollmentRepository;
import vn.edu.fpt.myfschool.service.EnrollmentPromotionService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("enrollmentPromotionService")
@RequiredArgsConstructor
@Transactional
public class EnrollmentPromotionServiceImpl implements EnrollmentPromotionService {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final AcademicYearRepository academicYearRepository;

    @Override
    public PromoteResponse promoteAll(Long fromAcademicYearId, Long toAcademicYearId) {
        AcademicYear fromYear = academicYearRepository.findById(fromAcademicYearId)
            .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", fromAcademicYearId));
        AcademicYear toYear = academicYearRepository.findById(toAcademicYearId)
            .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", toAcademicYearId));

        Map<String, String> classMapping = buildClassMapping(fromYear, toYear);
        List<Enrollment> enrollmentsToPromote = new ArrayList<>();
        List<SchoolClass> fromClasses = classRepository.findByAcademicYearId(fromYear.getId());
        for (SchoolClass cls : fromClasses) {
            enrollmentsToPromote.addAll(
                enrollmentRepository.findByClsIdAndAcademicYearIdAndStatus(cls.getId(), fromYear.getId(), EnrollmentStatus.ACTIVE)
            );
        }

        int promoted = 0, skipped = 0;
        List<String> warnings = new ArrayList<>();

        for (Enrollment e : enrollmentsToPromote) {
            String oldClassName = e.getCls().getName();
            String newClassName = classMapping.get(oldClassName);
            if (newClassName == null) {
                warnings.add(e.getStudent().getStudentCode() + ": Không tìm thấy lớp chuyển tiếp tương ứng cho " + oldClassName);
                skipped++;
                continue;
            }

            SchoolClass newClass = classRepository
                .findByNameAndAcademicYearId(newClassName, toYear.getId())
                .orElse(null);
            if (newClass == null) {
                warnings.add("Lớp " + newClassName + " ở năm học mới chưa được tạo.");
                skipped++;
                continue;
            }

            if (enrollmentRepository.findByStudentIdAndAcademicYearIdAndStatus(
                    e.getStudent().getId(), toYear.getId(), EnrollmentStatus.ACTIVE).isPresent()) {
                skipped++;
                continue;
            }

            e.setLeaveDate(fromYear.getEndDate());
            e.setStatus(EnrollmentStatus.PROMOTED);
            enrollmentRepository.save(e);

            Enrollment newEnrollment = new Enrollment();
            newEnrollment.setStudent(e.getStudent());
            newEnrollment.setCls(newClass);
            newEnrollment.setAcademicYear(toYear);
            newEnrollment.setJoinDate(toYear.getStartDate());
            newEnrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollmentRepository.save(newEnrollment);

            promoted++;
        }

        return new PromoteResponse(promoted, skipped, warnings);
    }

    private Map<String, String> buildClassMapping(AcademicYear fromYear, AcademicYear toYear) {
        List<SchoolClass> fromClasses = classRepository.findByAcademicYearId(fromYear.getId());
        List<SchoolClass> toClasses = classRepository.findByAcademicYearId(toYear.getId());

        Map<String, String> mapping = new HashMap<>();
        for (SchoolClass from : fromClasses) {
            String fromName = from.getName();
            String gradePrefix = String.valueOf(from.getGradeLevel());
            if (fromName.startsWith(gradePrefix)) {
                String suffix = fromName.substring(gradePrefix.length());
                String expectedNewName = (from.getGradeLevel() + 1) + suffix;
                mapping.put(fromName, expectedNewName);
            } else {
                String nonNumericPart = fromName.replaceAll("\\d+", "");
                SchoolClass target = toClasses.stream()
                    .filter(tc -> tc.getGradeLevel() == from.getGradeLevel() + 1)
                    .filter(tc -> tc.getName().contains(nonNumericPart))
                    .findFirst().orElse(null);
                if (target != null) {
                    mapping.put(fromName, target.getName());
                }
            }
        }
        return mapping;
    }
}
