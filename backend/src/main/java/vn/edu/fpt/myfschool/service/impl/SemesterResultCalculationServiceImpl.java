package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.CalculateSemesterResultResponse;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.controller.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.SemesterResultCalculationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service("semesterResultCalculationService")
@RequiredArgsConstructor
@Transactional
public class SemesterResultCalculationServiceImpl implements SemesterResultCalculationService {

    private final SemesterResultRepository semesterResultRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GradeRepository gradeRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceDetailRepository attendanceDetailRepository;
    private final ClassRepository classRepository;
    private final SemesterRepository semesterRepository;

    @Override
    public CalculateSemesterResultResponse calculate(Long classId, Long semesterId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));

        List<Enrollment> enrollments = enrollmentRepository
            .findByClsIdAndAcademicYearIdAndStatus(classId, cls.getAcademicYear().getId(), EnrollmentStatus.ACTIVE);

        int processed = 0, updated = 0, skipped = 0;
        List<String> warnings = new ArrayList<>();

        // Calculate GPA for each student
        List<StudentGPA> studentGPAs = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Student student = enrollment.getStudent();
            BigDecimal gpa = calculateGPA(student.getId(), semesterId);
            if (gpa == null) {
                warnings.add(student.getStudentCode() + ": khong co diem");
                skipped++;
                continue;
            }
            studentGPAs.add(new StudentGPA(student, gpa));
            processed++;
        }

        // Sort by GPA descending for ranking
        studentGPAs.sort(Comparator.comparing(StudentGPA::gpa).reversed());

        // Save results
        for (int i = 0; i < studentGPAs.size(); i++) {
            StudentGPA sg = studentGPAs.get(i);
            int rank = i + 1;

            SemesterResult result = semesterResultRepository
                .findByStudentIdAndSemesterId(sg.student().getId(), semesterId)
                .orElseGet(SemesterResult::new);

            result.setStudent(sg.student());
            result.setSemester(semester);
            result.setCls(cls);
            result.setGpa(sg.gpa());
            result.setRank(rank);
            result.setHonor(calculateHonor(sg.gpa()));
            result.setAcademicAbility(calculateAcademicAbility(sg.gpa()));
            result.setConduct(calculateConduct(sg.student().getId(), semester));
            semesterResultRepository.save(result);
            updated++;
        }

        return new CalculateSemesterResultResponse(processed, updated, skipped, warnings);
    }

    private BigDecimal calculateGPA(Long studentId, Long semesterId) {
        List<Grade> grades = gradeRepository.findByStudentIdAndSemesterId(studentId, semesterId);
        if (grades.isEmpty()) return null;

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (Grade g : grades) {
            if (g.getAverage() != null) {
                sum = sum.add(g.getAverage());
                count++;
            }
        }
        if (count == 0) return null;
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private String calculateHonor(BigDecimal gpa) {
        if (gpa.compareTo(BigDecimal.valueOf(8.0)) >= 0) return "Gioi";
        if (gpa.compareTo(BigDecimal.valueOf(6.5)) >= 0) return "Kha";
        if (gpa.compareTo(BigDecimal.valueOf(5.0)) >= 0) return "Trung binh";
        return "Yeu";
    }

    private String calculateAcademicAbility(BigDecimal gpa) {
        return calculateHonor(gpa);
    }

    private String calculateConduct(Long studentId, Semester semester) {
        // Placeholder: default to "Tot" until AttendanceSession data is available
        return "Tot";
    }

    private record StudentGPA(Student student, BigDecimal gpa) {}
}