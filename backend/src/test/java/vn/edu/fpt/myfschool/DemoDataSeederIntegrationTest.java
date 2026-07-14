package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.TimetableStatus;
import vn.edu.fpt.myfschool.config.DemoDataSeeder;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.AttendanceSession;
import vn.edu.fpt.myfschool.entity.Schedule;
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
@Transactional
class DemoDataSeederIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private AcademicYearRepository academicYearRepository;
    @Autowired private ClassRepository classRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private TeachingAssignmentRepository teachingAssignmentRepository;
    @Autowired private TimetableRepository timetableRepository;
    @Autowired private AttendanceSessionRepository attendanceSessionRepository;
    @Autowired private AttendanceDetailRepository attendanceDetailRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private GradeBookRepository gradeBookRepository;
    @Autowired private StudentScoreRepository studentScoreRepository;
    @Autowired private TuitionBillRepository tuitionBillRepository;
    @Autowired private AnnouncementRepository announcementRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SemesterRepository semesterRepository;
    @Autowired private HomeroomAssignmentRepository homeroomAssignmentRepository;

    @Test
    void seedCreatesCrossRoleCrossYearScenarioAndValidCredentials() {
        User admin = userRepository.findByPhone(DemoDataSeeder.ADMIN_PHONE).orElseThrow();
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(passwordEncoder.matches("Demo@123", admin.getPassword())).isTrue();

        User teacher = userRepository.findByPhone("0901000001").orElseThrow();
        User parent = userRepository.findByPhone("0902000001").orElseThrow();
        User student = userRepository.findByPhone("0903000001").orElseThrow();
        assertThat(teacher.getRole()).isEqualTo(UserRole.TEACHER);
        assertThat(parent.getRole()).isEqualTo(UserRole.PARENT);
        assertThat(student.getRole()).isEqualTo(UserRole.STUDENT);
        assertThat(passwordEncoder.matches("Demo@123", teacher.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("Demo@123", parent.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("Demo@123", student.getPassword())).isTrue();
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

        var class12A1 = classRepository.findByAcademicYearId(current.getId()).stream()
            .filter(cls -> "12A1".equals(cls.getName()))
            .findFirst().orElseThrow();
        var homeroom = homeroomAssignmentRepository
            .findByClsIdAndAcademicYearId(class12A1.getId(), current.getId()).stream()
            .findFirst().orElseThrow();
        assertThat(homeroom.getTeacher().getUser().getName()).isEqualTo("Nguyễn Thu Hà");
        assertThat(homeroom.getTeacher().getUser().getPhone()).isEqualTo("0901000001");

        attendanceSessionRepository.findAll().forEach(session -> {
            assertThat(session.getDate()).isBetween(
                session.getCls().getAcademicYear().getStartDate(),
                session.getCls().getAcademicYear().getEndDate());
            if (session.getSchedule() != null) {
                assertThat(session.getDate()).isBetween(
                    session.getSchedule().getTimetable().getSemester().getStartDate(),
                    session.getSchedule().getTimetable().getSemester().getEndDate());
            }
            attendanceDetailRepository.findBySessionId(session.getId()).forEach(detail -> {
                var canonical = attendanceRepository.findByStudentIdAndDateAndShift(
                    detail.getStudent().getId(), session.getDate(), session.getShift()).orElseThrow();
                assertThat(canonical.getStatus()).isEqualTo(detail.getStatus());
            });
        });
        leaveRequestRepository.findAll().forEach(request -> {
            assertThat(request.getDateFrom()).isBetween(
                request.getAcademicYear().getStartDate(), request.getAcademicYear().getEndDate());
            assertThat(request.getDateTo()).isBetween(
                request.getAcademicYear().getStartDate(), request.getAcademicYear().getEndDate());
            assertThat(semesterRepository.findByAcademicYearId(request.getAcademicYear().getId()))
                .anyMatch(semester -> !request.getDateFrom().isBefore(semester.getStartDate())
                    && !request.getDateTo().isAfter(semester.getEndDate()));
        });
        tuitionBillRepository.findAll().forEach(bill -> {
            assertThat(bill.getCls().getAcademicYear().getId())
                .isEqualTo(bill.getSemester().getAcademicYear().getId());
            assertThat(bill.getDueDate()).isBetween(
                bill.getSemester().getStartDate(), bill.getSemester().getEndDate());
        });
    }

    @Test
    void seededAttendanceMatchesEffectiveTimetableAndCanonicalRecords() {
        var sessions = attendanceSessionRepository.findAll();
        assertThat(sessions).isNotEmpty();

        long projectedDetails = 0;
        for (AttendanceSession session : sessions) {
            Schedule schedule = session.getSchedule();
            assertThat(schedule).as("session %s schedule", session.getId()).isNotNull();
            assertThat(schedule.getTimetable().getStatus())
                .isIn(TimetableStatus.ACTIVE, TimetableStatus.ARCHIVED);
            assertThat(schedule.getTimetable().getCls().getId())
                .isEqualTo(session.getCls().getId());
            assertThat(schedule.getTimetable().getSemester().getAcademicYear().getId())
                .isEqualTo(session.getCls().getAcademicYear().getId());
            assertThat(schedule.getShift()).isEqualTo(session.getShift());
            assertThat(schedule.getDayOfWeek()).isEqualTo(
                session.getDate().getDayOfWeek().getValue() % 7 + 1);
            assertThat(session.getDate()).isBetween(
                schedule.getTimetable().getSemester().getStartDate(),
                schedule.getTimetable().getSemester().getEndDate());
            assertThat(session.getDate()).isAfterOrEqualTo(
                schedule.getTimetable().getEffectiveFrom());
            if (schedule.getTimetable().getEffectiveTo() != null) {
                assertThat(session.getDate()).isBeforeOrEqualTo(
                    schedule.getTimetable().getEffectiveTo());
            }

            var effective = timetableRepository.findEffective(
                session.getCls().getId(),
                schedule.getTimetable().getSemester().getId(),
                session.getDate());
            assertThat(effective).isNotEmpty();
            assertThat(effective.get(0).getId())
                .isEqualTo(schedule.getTimetable().getId());

            var details = attendanceDetailRepository.findBySessionId(session.getId());
            projectedDetails += details.size();
            long present = details.stream()
                .filter(detail -> detail.getStatus() == AttendanceStatus.PRESENT)
                .count();
            assertThat(session.getTotal()).isEqualTo(details.size());
            assertThat(session.getPresent()).isEqualTo((int) present);
            assertThat(session.getAbsent()).isEqualTo(details.size() - (int) present);

            var canonicalRecords = attendanceRepository.findByClsIdAndDateAndShift(
                session.getCls().getId(), session.getDate(), session.getShift());
            assertThat(canonicalRecords).hasSameSizeAs(details);
            for (var detail : details) {
                var canonical = attendanceRepository.findByStudentIdAndDateAndShift(
                    detail.getStudent().getId(), session.getDate(), session.getShift())
                    .orElseThrow();
                assertThat(canonical.getCls().getId()).isEqualTo(session.getCls().getId());
                assertThat(canonical.getSchedule()).isNotNull();
                assertThat(canonical.getSchedule().getId()).isEqualTo(schedule.getId());
                assertThat(canonical.getStatus()).isEqualTo(detail.getStatus());
                if (canonical.getStatus() == AttendanceStatus.ABSENT_WITH_LEAVE) {
                    assertThat(canonical.getLeaveRequest()).isNotNull();
                }
            }
        }

        assertThat(attendanceRepository.count()).isEqualTo(projectedDetails);
        assertThat(attendanceDetailRepository.count()).isEqualTo(projectedDetails);
    }
}
