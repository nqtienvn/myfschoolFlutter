package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.config.DemoDataSeeder;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "app.demo-data.enabled=true",
    "app.demo-data.password=Demo@123",
    "spring.datasource.url=jdbc:h2:mem:demo_seed;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_LOWER=TRUE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "logging.level.org.hibernate.SQL=OFF"
})
@ActiveProfiles("test")
class DemoDataSeederIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private AcademicYearRepository academicYearRepository;
    @Autowired private ClassRepository classRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private TeachingAssignmentRepository teachingAssignmentRepository;
    @Autowired private TimetableRepository timetableRepository;
    @Autowired private AttendanceSessionRepository attendanceSessionRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private GradeBookRepository gradeBookRepository;
    @Autowired private StudentScoreRepository studentScoreRepository;
    @Autowired private TuitionBillRepository tuitionBillRepository;
    @Autowired private AnnouncementRepository announcementRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void seedCreatesCrossRoleCrossYearScenarioAndValidCredentials() {
        User admin = userRepository.findByPhone(DemoDataSeeder.ADMIN_PHONE).orElseThrow();
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(passwordEncoder.matches("Demo@123", admin.getPassword())).isTrue();

        assertThat(userRepository.findByPhone("0901000001").orElseThrow().getRole())
            .isEqualTo(UserRole.TEACHER);
        assertThat(userRepository.findByPhone("0902000001").orElseThrow().getRole())
            .isEqualTo(UserRole.PARENT);
        assertThat(userRepository.findByPhone("0903000001").orElseThrow().getRole())
            .isEqualTo(UserRole.STUDENT);
        assertThat(userRepository.findByPhone("0902000099").orElseThrow().getStatus())
            .isEqualTo(UserStatus.LOCKED);

        AcademicYear previous = academicYearRepository.findByName(DemoDataSeeder.PREVIOUS_YEAR_NAME)
            .orElseThrow();
        AcademicYear current = academicYearRepository.findByName(DemoDataSeeder.CURRENT_YEAR_NAME)
            .orElseThrow();
        AcademicYear draft = academicYearRepository.findByName(DemoDataSeeder.DRAFT_YEAR_NAME)
            .orElseThrow();

        assertThat(current.getName()).isEqualTo(
            current.getStartDate().getYear() + "-" + current.getEndDate().getYear()
        );
        assertThat(classRepository.findByAcademicYearId(previous.getId())).hasSize(1);
        assertThat(classRepository.findByAcademicYearId(current.getId())).hasSize(3);
        assertThat(classRepository.findByAcademicYearId(draft.getId())).isEmpty();

        assertThat(enrollmentRepository.count()).isGreaterThanOrEqualTo(13);
        assertThat(teachingAssignmentRepository.count()).isGreaterThanOrEqualTo(14);
        assertThat(timetableRepository.count()).isGreaterThanOrEqualTo(4);
        assertThat(attendanceSessionRepository.count()).isGreaterThanOrEqualTo(8);
        assertThat(leaveRequestRepository.count()).isGreaterThanOrEqualTo(3);
        assertThat(gradeBookRepository.count()).isGreaterThanOrEqualTo(6);
        assertThat(studentScoreRepository.count()).isGreaterThanOrEqualTo(80);
        assertThat(tuitionBillRepository.count()).isGreaterThanOrEqualTo(19);
        assertThat(announcementRepository.count()).isGreaterThanOrEqualTo(6);
        assertThat(conversationRepository.count()).isGreaterThanOrEqualTo(3);
        assertThat(notificationRepository.count()).isGreaterThanOrEqualTo(10);
    }
}
