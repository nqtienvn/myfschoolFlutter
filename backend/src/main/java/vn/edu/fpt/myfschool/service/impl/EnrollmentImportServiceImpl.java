package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.myfschool.common.dto.ImportResultDto;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.common.util.ExcelReader;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.Enrollment;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.repository.ClassRepository;
import vn.edu.fpt.myfschool.repository.EnrollmentRepository;
import vn.edu.fpt.myfschool.repository.StudentRepository;
import vn.edu.fpt.myfschool.service.EnrollmentImportService;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service("enrollmentImportService")
@RequiredArgsConstructor
@Transactional
public class EnrollmentImportServiceImpl implements EnrollmentImportService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ExcelReader excelReader;

    @Override
    public ImportResultDto importFromExcel(MultipartFile file, Long academicYearId) {
        AcademicYear year = academicYearRepository.findById(academicYearId)
            .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", academicYearId));

        List<Map<String, String>> rows;
        try (InputStream is = file.getInputStream()) {
            rows = excelReader.read(is);
        } catch (Exception e) {
            return new ImportResultDto(0, 0, 0, List.of("Không thể đọc tệp Excel: " + e.getMessage()));
        }

        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 2;
            try {
                String studentCode = row.get("studentCode");
                String className = row.get("className");

                if (studentCode == null || studentCode.trim().isEmpty()) {
                    throw new IllegalArgumentException("Mã học sinh trống");
                }
                if (className == null || className.trim().isEmpty()) {
                    throw new IllegalArgumentException("Tên lớp trống");
                }

                Student student = studentRepository.findByStudentCode(studentCode.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học sinh: " + studentCode));
                SchoolClass cls = classRepository.findByNameAndAcademicYearId(className.trim(), year.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp " + className + " trong năm học " + year.getName()));

                if (enrollmentRepository.findByStudentIdAndAcademicYearIdAndStatus(
                        student.getId(), year.getId(), EnrollmentStatus.ACTIVE).isPresent()) {
                    throw new IllegalArgumentException("Học sinh đã có lớp học hoạt động trong năm học này");
                }

                LocalDate joinDate = year.getStartDate();
                if (row.containsKey("joinDate") && !row.get("joinDate").trim().isEmpty()) {
                    try {
                        joinDate = LocalDate.parse(row.get("joinDate").trim());
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Định dạng ngày nhập học không hợp lệ (phải là YYYY-MM-DD)");
                    }
                }

                Enrollment enrollment = new Enrollment();
                enrollment.setStudent(student);
                enrollment.setCls(cls);
                enrollment.setAcademicYear(year);
                enrollment.setJoinDate(joinDate);
                enrollment.setStatus(EnrollmentStatus.ACTIVE);
                enrollmentRepository.save(enrollment);

                student.setCurrentClass(cls);
                studentRepository.save(student);

                success++;
            } catch (Exception e) {
                failed++;
                errors.add("Dòng " + rowNum + ": " + e.getMessage());
            }
        }

        return new ImportResultDto(rows.size(), success, failed, errors);
    }
}
