package vn.edu.fpt.myfschool.service;

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

public interface ClubRegistrationService {
    ClubRegistration register(String clubName, String academicYear, Long studentUserId);

    void cancel(Long registrationId, Long studentUserId);

    List<ClubRegistration> getMyRegistrations(Long studentUserId, String academicYear);
}
