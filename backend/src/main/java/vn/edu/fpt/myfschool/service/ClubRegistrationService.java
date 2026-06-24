package vn.edu.fpt.myfschool.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.enums.ClubStatus;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.ClubRegistration;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.repository.ClubRegistrationRepository;
import vn.edu.fpt.myfschool.repository.StudentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClubRegistrationService {

    private final ClubRegistrationRepository clubRegistrationRepository;
    private final StudentRepository studentRepository;

    public ClubRegistration register(String clubName, String academicYear, Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", studentUserId));

        if (clubRegistrationRepository.existsByStudentIdAndClubNameAndAcademicYear(
                student.getId(), clubName, academicYear)) {
            throw new ConflictException("Đã đăng ký CLB này");
        }

        ClubRegistration cr = new ClubRegistration();
        cr.setStudent(student);
        cr.setClubName(clubName);
        cr.setAcademicYear(academicYear);
        cr.setStatus(ClubStatus.REGISTERED);
        cr.setRegisteredAt(LocalDateTime.now());
        return clubRegistrationRepository.save(cr);
    }

    public void cancel(Long registrationId, Long studentUserId) {
        ClubRegistration cr = clubRegistrationRepository.findById(registrationId)
            .orElseThrow(() -> new ResourceNotFoundException("ClubRegistration", "id", registrationId));
        cr.setStatus(ClubStatus.CANCELLED);
        clubRegistrationRepository.save(cr);
    }

    @Transactional(readOnly = true)
    public List<ClubRegistration> getMyRegistrations(Long studentUserId, String academicYear) {
        Student student = studentRepository.findByUserId(studentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", studentUserId));
        return clubRegistrationRepository.findByStudentIdAndAcademicYearOrderByRegisteredAtDesc(
            student.getId(), academicYear);
    }
}
