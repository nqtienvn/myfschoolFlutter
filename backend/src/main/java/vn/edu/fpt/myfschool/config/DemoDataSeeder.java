package vn.edu.fpt.myfschool.config;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.enums.*;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates an isolated, cross-feature data set for local manual testing.
 *
 * <p>The seed is deliberately implemented through JPA instead of a data.sql file so it follows
 * the current entity model on both MySQL and H2. All operational records are attached to an
 * academic year, which also makes the data useful for testing the admin year's scope boundary.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String ADMIN_PHONE = "0868589707";
    public static final String CURRENT_YEAR_NAME = "2026-2027";
    public static final String PREVIOUS_YEAR_NAME = "2025-2026";
    public static final String DRAFT_YEAR_NAME = "2027-2028";

    private static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final String demoPassword;

    public DemoDataSeeder(EntityManager entityManager,
                          PasswordEncoder passwordEncoder,
                          UserRepository userRepository,
                          @Value("${app.demo-data.password:Demo@123}") String demoPassword) {
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByPhone(ADMIN_PHONE)) {
            log.info("Demo data already exists (sentinel account {}). Skipping seed.", ADMIN_PHONE);
            return;
        }

        log.info("Creating comprehensive MyFschool demo data...");

        // Global catalogs.
        School school = createSchool();
        createGradeLevels();
        SchoolShift morning = findOrCreateShift("Buổi sáng", "MORNING", 1);
        SchoolShift afternoon = findOrCreateShift("Buổi chiều", "AFTERNOON", 2);
        List<Period> periods = createPeriods(morning, afternoon);
        createRooms();
        Map<String, Subject> subjects = createSubjects();
        GradeConfigTemplate gradeTemplate = createGradeTemplate();

        // Three lifecycle states make the admin year selector useful for both positive and
        // negative validation paths.
        AcademicYear previousYear = createAcademicYear(
            PREVIOUS_YEAR_NAME,
            LocalDate.of(2025, 7, 1),
            LocalDate.of(2026, 6, 30),
            AcademicYearStatus.COMPLETED
        );
        AcademicYear currentYear = createAcademicYear(
            CURRENT_YEAR_NAME,
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2027, 6, 30),
            AcademicYearStatus.ACTIVE
        );
        createAcademicYear(
            DRAFT_YEAR_NAME,
            LocalDate.of(2027, 7, 1),
            LocalDate.of(2028, 6, 30),
            AcademicYearStatus.DRAFT
        );

        applyCatalog(previousYear, subjects, List.of(morning, afternoon), periods);
        applyCatalog(currentYear, subjects, List.of(morning, afternoon), periods);
        AcademicYearGradeConfig previousConfig = createYearGradeConfig(previousYear, gradeTemplate);
        AcademicYearGradeConfig currentConfig = createYearGradeConfig(currentYear, gradeTemplate);

        createSemester(
            previousYear, "Học kỳ I", 1,
            LocalDate.of(2025, 7, 1), LocalDate.of(2025, 12, 31),
            false, SemesterStatus.COMPLETED
        );
        Semester previousSemester2 = createSemester(
            previousYear, "Học kỳ II", 2,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
            false, SemesterStatus.COMPLETED
        );
        Semester currentSemester1 = createSemester(
            currentYear, "Học kỳ I", 1,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31),
            true, SemesterStatus.ACTIVE
        );
        createSemester(
            currentYear, "Học kỳ II", 2,
            LocalDate.of(2027, 1, 1), LocalDate.of(2027, 6, 30),
            false, SemesterStatus.NOT_STARTED
        );

        SchoolClass previous11A1 = createClass(previousYear, school, "11A1", 11);
        SchoolClass class12A1 = createClass(currentYear, school, "12A1", 12);
        SchoolClass class12A2 = createClass(currentYear, school, "12A2", 12);
        SchoolClass class10A1 = createClass(currentYear, school, "10A1", 10);

        User admin = createUser(
            ADMIN_PHONE, "Quản trị Demo", "admin.demo@myfschool.vn", "001086858970",
            UserRole.ADMIN, UserStatus.ACTIVE, false
        );

        Teacher teacherMath = createTeacher(
            "0901000001", "Nguyễn Thu Hà", "teacher.math.demo@myfschool.vn", "GV-DEMO-01",
            UserStatus.ACTIVE, Set.of(subjects.get("TOAN"), subjects.get("TIN"))
        );
        Teacher teacherLiterature = createTeacher(
            "0901000002", "Trần Minh Anh", "teacher.literature.demo@myfschool.vn", "GV-DEMO-02",
            UserStatus.ACTIVE, Set.of(subjects.get("NGUVAN"))
        );
        Teacher teacherEnglish = createTeacher(
            "0901000003", "Lê Hoàng Nam", "teacher.english.demo@myfschool.vn", "GV-DEMO-03",
            UserStatus.ACTIVE, Set.of(subjects.get("TIENGANH"))
        );
        Teacher teacherScience = createTeacher(
            "0901000004", "Phạm Ngọc Mai", "teacher.science.demo@myfschool.vn", "GV-DEMO-04",
            UserStatus.ACTIVE,
            Set.of(subjects.get("VATLY"), subjects.get("HOAHOC"), subjects.get("THEDUC"))
        );
        createTeacher(
            "0901000099", "Giáo viên đã nghỉ", "teacher.inactive.demo@myfschool.vn", "GV-DEMO-99",
            UserStatus.INACTIVE, Set.of(subjects.get("TOAN"))
        );

        Parent parentHung = createParent(
            "0902000001", "Nguyễn Văn Hùng", "parent.hung.demo@myfschool.vn",
            UserStatus.ACTIVE, "Kỹ sư", "Cầu Giấy, Hà Nội"
        );
        Parent parentLan = createParent(
            "0902000002", "Trần Thị Lan", "parent.lan.demo@myfschool.vn",
            UserStatus.ACTIVE, "Kế toán", "Nam Từ Liêm, Hà Nội"
        );
        Parent parentMinh = createParent(
            "0902000003", "Lê Quốc Minh", "parent.minh.demo@myfschool.vn",
            UserStatus.ACTIVE, "Kinh doanh", "Hà Đông, Hà Nội"
        );
        createParent(
            "0902000099", "Phụ huynh bị khóa", "parent.locked.demo@myfschool.vn",
            UserStatus.LOCKED, "", "Hà Nội"
        );

        Student studentAn = createStudent(
            "0903000001", "Nguyễn Minh An", "student.an.demo@myfschool.vn", "HS-DEMO-001",
            class12A1, LocalDate.of(2009, 9, 12), Gender.MALE
        );
        Student studentBinh = createStudent(
            "0903000002", "Trần Gia Bình", "student.binh.demo@myfschool.vn", "HS-DEMO-002",
            class12A1, LocalDate.of(2009, 3, 21), Gender.MALE
        );
        Student studentChi = createStudent(
            "0903000003", "Lê Mai Chi", "student.chi.demo@myfschool.vn", "HS-DEMO-003",
            class12A1, LocalDate.of(2009, 11, 5), Gender.FEMALE
        );
        Student studentDung = createStudent(
            "0903000004", "Phạm Đức Dũng", "student.dung.demo@myfschool.vn", "HS-DEMO-004",
            class12A1, LocalDate.of(2009, 7, 18), Gender.MALE
        );
        Student studentHa = createStudent(
            "0903000005", "Vũ Thanh Hà", "student.ha.demo@myfschool.vn", "HS-DEMO-005",
            class12A1, LocalDate.of(2009, 5, 9), Gender.FEMALE
        );
        Student studentLinh = createStudent(
            "0903000006", "Đỗ Khánh Linh", "student.linh.demo@myfschool.vn", "HS-DEMO-006",
            class12A2, LocalDate.of(2009, 2, 14), Gender.FEMALE
        );
        Student studentMinh = createStudent(
            "0903000007", "Bùi Nhật Minh", "student.minh.demo@myfschool.vn", "HS-DEMO-007",
            class12A2, LocalDate.of(2009, 12, 1), Gender.MALE
        );
        Student studentNam = createStudent(
            "0903000008", "Nguyễn Hoài Nam", "student.nam.demo@myfschool.vn", "HS-DEMO-008",
            class10A1, LocalDate.of(2011, 4, 23), Gender.MALE
        );

        List<Student> students12A1 = List.of(studentAn, studentBinh, studentChi, studentDung, studentHa);
        List<Student> students12A2 = List.of(studentLinh, studentMinh);

        for (Student student : students12A1) {
            createEnrollment(student, previous11A1, previousYear, LocalDate.of(2025, 7, 1),
                LocalDate.of(2026, 6, 30), EnrollmentStatus.PROMOTED);
            createEnrollment(student, class12A1, currentYear, LocalDate.of(2026, 7, 1),
                null, EnrollmentStatus.ACTIVE);
        }
        for (Student student : students12A2) {
            createEnrollment(student, class12A2, currentYear, LocalDate.of(2026, 7, 1),
                null, EnrollmentStatus.ACTIVE);
        }
        createEnrollment(studentNam, class10A1, currentYear, LocalDate.of(2026, 7, 1),
            null, EnrollmentStatus.ACTIVE);

        createGuardian(studentAn, parentHung, Relationship.FATHER);
        createGuardian(studentNam, parentHung, Relationship.FATHER);
        createGuardian(studentBinh, parentLan, Relationship.MOTHER);
        createGuardian(studentHa, parentLan, Relationship.GUARDIAN);
        createGuardian(studentLinh, parentLan, Relationship.MOTHER);
        createGuardian(studentChi, parentMinh, Relationship.FATHER);
        createGuardian(studentDung, parentMinh, Relationship.GUARDIAN);
        createGuardian(studentMinh, parentMinh, Relationship.FATHER);

        createHomeroom(previous11A1, teacherMath, previousYear, previousYear.getStartDate(), previousYear.getEndDate());
        createHomeroom(class12A1, teacherMath, currentYear, currentYear.getStartDate(), null);
        createHomeroom(class12A2, teacherLiterature, currentYear, currentYear.getStartDate(), null);
        createHomeroom(class10A1, teacherEnglish, currentYear, currentYear.getStartDate(), null);

        Map<String, TeachingAssignment> previousAssignments = new LinkedHashMap<>();
        previousAssignments.put("TOAN", createAssignment(previous11A1, subjects.get("TOAN"), teacherMath, previousYear));
        previousAssignments.put("NGUVAN", createAssignment(previous11A1, subjects.get("NGUVAN"), teacherLiterature, previousYear));

        Map<String, TeachingAssignment> assignments12A1 = new LinkedHashMap<>();
        assignments12A1.put("TOAN", createAssignment(class12A1, subjects.get("TOAN"), teacherMath, currentYear));
        assignments12A1.put("NGUVAN", createAssignment(class12A1, subjects.get("NGUVAN"), teacherLiterature, currentYear));
        assignments12A1.put("TIENGANH", createAssignment(class12A1, subjects.get("TIENGANH"), teacherEnglish, currentYear));
        assignments12A1.put("VATLY", createAssignment(class12A1, subjects.get("VATLY"), teacherScience, currentYear));
        assignments12A1.put("HOAHOC", createAssignment(class12A1, subjects.get("HOAHOC"), teacherScience, currentYear));
        assignments12A1.put("TIN", createAssignment(class12A1, subjects.get("TIN"), teacherMath, currentYear));
        assignments12A1.put("THEDUC", createAssignment(class12A1, subjects.get("THEDUC"), teacherScience, currentYear));

        Map<String, TeachingAssignment> assignments12A2 = new LinkedHashMap<>();
        assignments12A2.put("TOAN", createAssignment(class12A2, subjects.get("TOAN"), teacherMath, currentYear));
        assignments12A2.put("NGUVAN", createAssignment(class12A2, subjects.get("NGUVAN"), teacherLiterature, currentYear));
        assignments12A2.put("TIENGANH", createAssignment(class12A2, subjects.get("TIENGANH"), teacherEnglish, currentYear));

        Map<String, TeachingAssignment> assignments10A1 = new LinkedHashMap<>();
        assignments10A1.put("TOAN", createAssignment(class10A1, subjects.get("TOAN"), teacherMath, currentYear));
        assignments10A1.put("TIENGANH", createAssignment(class10A1, subjects.get("TIENGANH"), teacherEnglish, currentYear));

        Timetable historicTimetable = createTimetable(
            previous11A1, previousSemester2, 1, TimetableStatus.ARCHIVED,
            previousSemester2.getStartDate(), previousSemester2.getEndDate()
        );
        populateTimetable(historicTimetable, new ArrayList<>(previousAssignments.values()), periods, 2, 6, 1, 4);

        Timetable activeTimetable = createTimetable(
            class12A1, currentSemester1, 1, TimetableStatus.ACTIVE,
            currentSemester1.getStartDate(), currentSemester1.getEndDate()
        );
        populateTimetable(activeTimetable, new ArrayList<>(assignments12A1.values()), periods, 2, 6, 1, 8);

        Timetable draftTimetable = createTimetable(
            class12A2, currentSemester1, 1, TimetableStatus.DRAFT,
            currentSemester1.getStartDate(), currentSemester1.getEndDate()
        );
        populateTimetable(draftTimetable, new ArrayList<>(assignments12A2.values()), periods, 2, 2, 1, 5);

        Timetable scheduledTimetable = createTimetable(
            class10A1, currentSemester1, 1, TimetableStatus.SCHEDULED,
            currentSemester1.getStartDate().plusDays(7), currentSemester1.getEndDate()
        );
        populateTimetable(scheduledTimetable, new ArrayList<>(assignments10A1.values()), periods, 2, 3, 1, 5);

        // Anchor all dated demo operations inside the selected semester. Using the
        // machine clock here made the fixed 2026-2027 seed invalid after that year.
        LocalDate demoReferenceDate = currentSemester1.getStartDate().plusDays(14);

        // Leave requests first so approved leave can be linked to attendance.
        List<LocalDate> recentSchoolDays = recentSchoolDays(demoReferenceDate, 6);
        LocalDate approvedLeaveDate = recentSchoolDays.get(recentSchoolDays.size() - 3);
        LeaveRequest approvedLeave = createLeaveRequest(
            studentAn, parentHung, class12A1, currentYear, teacherMath,
            approvedLeaveDate, approvedLeaveDate, LeaveShift.MORNING,
            "Khám sức khỏe theo lịch hẹn.", LeaveStatus.APPROVED,
            "Đã xác nhận. Phụ huynh nhắc em bổ sung bài học.", LocalDateTime.now(SCHOOL_ZONE).minusDays(2)
        );
        createAttachment(approvedLeave, null, "/demo/attachments/giay-kham-suc-khoe.pdf",
            "giay-kham-suc-khoe.pdf", 182_400, "application/pdf");
        createLeaveRequest(
            studentNam, parentHung, class10A1, currentYear, null,
            nextSchoolDay(demoReferenceDate), nextSchoolDay(demoReferenceDate),
            LeaveShift.FULL_DAY, "Gia đình có việc riêng.", LeaveStatus.PENDING, null, null
        );
        createLeaveRequest(
            studentBinh, parentLan, class12A1, currentYear, teacherMath,
            nextSchoolDay(demoReferenceDate).plusDays(2),
            nextSchoolDay(demoReferenceDate).plusDays(2),
            LeaveShift.AFTERNOON, "Tham gia hoạt động bên ngoài trường.", LeaveStatus.REJECTED,
            "Lịch hoạt động trùng với bài kiểm tra giữa kỳ.", LocalDateTime.now(SCHOOL_ZONE).minusHours(6)
        );

        createAttendanceHistory(
            students12A1, class12A1, teacherMath, recentSchoolDays,
            activeTimetable, approvedLeave, approvedLeaveDate
        );
        createAttendanceCorrectionRequests(class12A1, teacherMath, recentSchoolDays, studentDung);

        // Grade books intentionally cover all workflow states and contain both completed and
        // missing scores so empty/error UI states can be exercised.
        GradeBook previousMathBook = createGradeBook(
            previous11A1, subjects.get("TOAN"), previousSemester2, previousConfig,
            GradeBookStatus.LOCKED, true
        );
        seedScores(previousMathBook, students12A1, teacherMath.getUser(), admin, 4);
        GradeBook previousLiteratureBook = createGradeBook(
            previous11A1, subjects.get("NGUVAN"), previousSemester2, previousConfig,
            GradeBookStatus.PUBLISHED, true
        );
        seedScores(previousLiteratureBook, students12A1, teacherLiterature.getUser(), admin, 4);

        GradeBook currentMathBook = createGradeBook(
            class12A1, subjects.get("TOAN"), currentSemester1, currentConfig,
            GradeBookStatus.PUBLISHED, true
        );
        List<StudentScore> currentMathScores = seedScores(
            currentMathBook, students12A1, teacherMath.getUser(), admin, 4
        );
        GradeBook currentLiteratureBook = createGradeBook(
            class12A1, subjects.get("NGUVAN"), currentSemester1, currentConfig,
            GradeBookStatus.SUBMITTED, false
        );
        seedScores(currentLiteratureBook, students12A1, teacherLiterature.getUser(), admin, 3);
        GradeBook currentEnglishBook = createGradeBook(
            class12A1, subjects.get("TIENGANH"), currentSemester1, currentConfig,
            GradeBookStatus.DRAFT, false
        );
        seedScores(currentEnglishBook, students12A1, teacherEnglish.getUser(), admin, 1);
        GradeBook class12A2MathBook = createGradeBook(
            class12A2, subjects.get("TOAN"), currentSemester1, currentConfig,
            GradeBookStatus.DRAFT, false
        );
        seedScores(class12A2MathBook, students12A2, teacherMath.getUser(), admin, 2);

        createScoreAudit(currentMathScores.get(0), new BigDecimal("7.50"),
            currentMathScores.get(0).getScore(), teacherMath.getUser(), "Điều chỉnh sau khi phúc khảo.");
        createSemesterResults(students12A1, previous11A1, previousSemester2, new BigDecimal("7.80"));
        createSemesterResults(students12A1, class12A1, currentSemester1, new BigDecimal("8.10"));
        createSemesterResults(students12A2, class12A2, currentSemester1, new BigDecimal("7.50"));

        Map<String, TuitionBill> highlightedBills = createFinanceData(
            currentSemester1, class12A1, class12A2, class10A1,
            students12A1, students12A2, studentNam
        );

        createAnnouncementPolicy(currentYear, admin);
        Map<String, Announcement> announcements = createAnnouncements(
            currentYear, admin, teacherMath, teacherLiterature,
            class12A1, class12A2, class10A1
        );
        createAnnouncementRead(announcements.get("MEETING"), studentAn.getUser(), LocalDateTime.now(SCHOOL_ZONE).minusHours(3));
        createAnnouncementRead(announcements.get("EXAM"), parentHung.getUser(), LocalDateTime.now(SCHOOL_ZONE).minusHours(1));

        createCommunicationData(
            teacherMath.getUser(), teacherLiterature.getUser(), parentHung.getUser(),
            studentAn.getUser(), studentChi.getUser()
        );
        createNotifications(
            admin, teacherMath.getUser(), parentHung.getUser(), studentAn.getUser(),
            highlightedBills, approvedLeave, announcements
        );
        createAuditLogs(admin);

        entityManager.flush();
        log.info(
            "Demo data ready. Login accounts: admin={}, teacher=0901000001, parent=0902000001, " +
                "student=0903000001 (shared password configured by app.demo-data.password).",
            ADMIN_PHONE
        );
    }

    private School createSchool() {
        School school = new School();
        school.setName("FPT Schools Cầu Giấy Demo");
        school.setCode("FPT-CG-DEMO");
        school.setAddress("15 Đông Quan, Cầu Giấy, Hà Nội");
        school.setPhone("02473005588");
        school.setSchoolName("FPT Schools");
        return persist(school);
    }

    private void createGradeLevels() {
        createGradeLevel("Khối 10", "K10", 10, "Khối đầu cấp THPT");
        createGradeLevel("Khối 11", "K11", 11, "Khối giữa cấp THPT");
        createGradeLevel("Khối 12", "K12", 12, "Khối cuối cấp THPT");
    }

    private GradeLevel createGradeLevel(String name, String code, int order, String description) {
        GradeLevel gradeLevel = new GradeLevel();
        gradeLevel.setName(name);
        gradeLevel.setCode(code);
        gradeLevel.setOrder(order);
        gradeLevel.setDescription(description);
        return persist(gradeLevel);
    }

    private SchoolShift findOrCreateShift(String name, String code, int order) {
        return entityManager.createQuery(
                "select s from SchoolShift s where s.code = :code", SchoolShift.class)
            .setParameter("code", code)
            .getResultStream()
            .findFirst()
            .orElseGet(() -> {
                SchoolShift shift = new SchoolShift();
                shift.setName(name);
                shift.setCode(code);
                shift.setOrder(order);
                return persist(shift);
            });
    }

    private List<Period> createPeriods(SchoolShift morning, SchoolShift afternoon) {
        List<Period> periods = new ArrayList<>();
        for (int order = 1; order <= 10; order++) {
            SchoolShift shift = order <= 5 ? morning : afternoon;
            int periodOrder = order;
            Period period = entityManager.createQuery(
                    "select p from Period p where p.shift.id = :shiftId and p.order = :order", Period.class)
                .setParameter("shiftId", shift.getId())
                .setParameter("order", periodOrder)
                .getResultStream()
                .findFirst()
                .orElseGet(() -> {
                    Period created = new Period();
                    created.setName("Tiết " + periodOrder);
                    created.setOrder(periodOrder);
                    created.setShift(shift);
                    created.setIsActive(true);
                    return persist(created);
                });
            periods.add(period);
        }
        return periods;
    }

    private void createRooms() {
        createRoom("D-A101", 35, "Tòa Alpha", "Máy chiếu, điều hòa");
        createRoom("D-A102", 35, "Tòa Alpha", "Màn hình tương tác");
        createRoom("D-LAB1", 30, "Tòa Beta", "30 máy tính, máy chiếu");
        createRoom("D-GYM", 60, "Nhà đa năng", "Dụng cụ thể thao");
    }

    private Room createRoom(String name, int capacity, String building, String equipment) {
        Room room = new Room();
        room.setName(name);
        room.setCapacity(capacity);
        room.setBuilding(building);
        room.setEquipment(equipment);
        room.setIsActive(true);
        return persist(room);
    }

    private Map<String, Subject> createSubjects() {
        Map<String, Subject> subjects = new LinkedHashMap<>();
        subjects.put("TOAN", findOrCreateSubject("Toán", "TOAN"));
        subjects.put("NGUVAN", findOrCreateSubject("Ngữ văn", "NGUVAN"));
        subjects.put("TIENGANH", findOrCreateSubject("Tiếng Anh", "TIENGANH"));
        subjects.put("VATLY", findOrCreateSubject("Vật lý", "VATLY"));
        subjects.put("HOAHOC", findOrCreateSubject("Hóa học", "HOAHOC"));
        subjects.put("TIN", findOrCreateSubject("Tin học", "TIN"));
        subjects.put("THEDUC", findOrCreateSubject("Giáo dục thể chất", "THEDUC"));
        return subjects;
    }

    private Subject findOrCreateSubject(String name, String code) {
        return entityManager.createQuery("select s from Subject s where s.code = :code", Subject.class)
            .setParameter("code", code)
            .getResultStream()
            .findFirst()
            .orElseGet(() -> {
                Subject subject = new Subject();
                subject.setName(name);
                subject.setCode(code);
                return persist(subject);
            });
    }

    private GradeConfigTemplate createGradeTemplate() {
        GradeConfigTemplate template = new GradeConfigTemplate();
        template.setName("[DEMO] Cấu hình điểm THPT");
        template.setVersion(1);
        template.setActive(true);
        addTemplateItem(template, "TX", "Thường xuyên", 1, 2, GradeEntryRole.SUBJECT_TEACHER, 0);
        addTemplateItem(template, "GK", "Giữa kỳ", 2, 1, GradeEntryRole.ADMIN, 1);
        addTemplateItem(template, "CK", "Cuối kỳ", 3, 1, GradeEntryRole.ADMIN, 2);
        return persist(template);
    }

    private void addTemplateItem(GradeConfigTemplate template, String code, String name,
                                 int weight, int quantity, GradeEntryRole role, int order) {
        GradeConfigTemplateItem item = new GradeConfigTemplateItem();
        item.setTemplate(template);
        item.setCode(code);
        item.setDisplayName(name);
        item.setWeight(weight);
        item.setQuantity(quantity);
        item.setEntryRole(role);
        item.setAssessmentType(AssessmentType.SCORE);
        item.setRequiredEntry(true);
        item.setDisplayOrder(order);
        template.getItems().add(item);
    }

    private AcademicYear createAcademicYear(String name, LocalDate start, LocalDate end,
                                            AcademicYearStatus status) {
        AcademicYear year = new AcademicYear();
        year.setName(name);
        year.setStartDate(start);
        year.setEndDate(end);
        year.setStatus(status);
        return persist(year);
    }

    private void applyCatalog(AcademicYear year, Map<String, Subject> subjects,
                              List<SchoolShift> shifts, List<Period> periods) {
        for (Subject subject : subjects.values()) {
            AcademicYearSubject applied = new AcademicYearSubject();
            applied.setAcademicYear(year);
            applied.setSubject(subject);
            persist(applied);
        }
        for (SchoolShift shift : shifts) {
            AcademicYearShift applied = new AcademicYearShift();
            applied.setAcademicYear(year);
            applied.setShift(shift);
            persist(applied);
        }
        for (Period period : periods) {
            AcademicYearPeriod applied = new AcademicYearPeriod();
            applied.setAcademicYear(year);
            applied.setPeriod(period);
            persist(applied);
        }
    }

    private AcademicYearGradeConfig createYearGradeConfig(AcademicYear year,
                                                           GradeConfigTemplate template) {
        AcademicYearGradeConfig config = new AcademicYearGradeConfig();
        config.setAcademicYear(year);
        config.setSourceTemplate(template);
        config.setStatus("VALIDATED");
        addYearConfigItem(config, "TX", "Thường xuyên", 1, 2, GradeEntryRole.SUBJECT_TEACHER, 0);
        addYearConfigItem(config, "GK", "Giữa kỳ", 2, 1, GradeEntryRole.ADMIN, 1);
        addYearConfigItem(config, "CK", "Cuối kỳ", 3, 1, GradeEntryRole.ADMIN, 2);
        return persist(config);
    }

    private void addYearConfigItem(AcademicYearGradeConfig config, String code, String name,
                                   int weight, int quantity, GradeEntryRole role, int order) {
        AcademicYearGradeConfigItem item = new AcademicYearGradeConfigItem();
        item.setConfig(config);
        item.setCode(code);
        item.setDisplayName(name);
        item.setWeight(weight);
        item.setQuantity(quantity);
        item.setEntryRole(role);
        item.setAssessmentType(AssessmentType.SCORE);
        item.setRequiredEntry(true);
        item.setDisplayOrder(order);
        config.getItems().add(item);
    }

    private Semester createSemester(AcademicYear year, String name, int order,
                                    LocalDate start, LocalDate end, boolean current,
                                    SemesterStatus status) {
        Semester semester = new Semester();
        semester.setAcademicYear(year);
        semester.setName(name);
        semester.setOrder(order);
        semester.setStartDate(start);
        semester.setEndDate(end);
        semester.setIsCurrent(current);
        semester.setStatus(status);
        semester.setDeleted(false);
        return persist(semester);
    }

    private SchoolClass createClass(AcademicYear year, School school, String name, int gradeLevel) {
        SchoolClass cls = new SchoolClass();
        cls.setAcademicYear(year);
        cls.setSchool(school);
        cls.setSchoolName(school.getSchoolName());
        cls.setName(name);
        cls.setGradeLevel(gradeLevel);
        return persist(cls);
    }

    private User createUser(String phone, String name, String email, String citizenId,
                            UserRole role, UserStatus status, boolean mustChangePassword) {
        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(demoPassword));
        user.setName(name);
        user.setEmail(email);
        user.setCitizenId(citizenId);
        user.setRole(role);
        user.setStatus(status);
        user.setMustChangePassword(mustChangePassword);
        return persist(user);
    }

    private Teacher createTeacher(String phone, String name, String email, String employeeCode,
                                  UserStatus status, Set<Subject> subjects) {
        User user = createUser(phone, name, email, "T-" + employeeCode,
            UserRole.TEACHER, status, false);
        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setEmployeeCode(employeeCode);
        teacher.getSubjects().addAll(subjects);
        return persist(teacher);
    }

    private Parent createParent(String phone, String name, String email, UserStatus status,
                                String occupation, String address) {
        User user = createUser(phone, name, email, "P-" + phone,
            UserRole.PARENT, status, false);
        Parent parent = new Parent();
        parent.setUser(user);
        parent.setOccupation(occupation);
        parent.setAddress(address);
        return persist(parent);
    }

    private Student createStudent(String phone, String name, String email, String studentCode,
                                  SchoolClass currentClass, LocalDate dateOfBirth, Gender gender) {
        User user = createUser(phone, name, email, "S-" + studentCode,
            UserRole.STUDENT, UserStatus.ACTIVE, false);
        Student student = new Student();
        student.setUser(user);
        student.setStudentCode(studentCode);
        student.setCurrentClass(currentClass);
        student.setDateOfBirth(dateOfBirth);
        student.setGender(gender);
        student.setAddress("Hà Nội");
        return persist(student);
    }

    private Enrollment createEnrollment(Student student, SchoolClass cls, AcademicYear year,
                                        LocalDate joinDate, LocalDate leaveDate,
                                        EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCls(cls);
        enrollment.setAcademicYear(year);
        enrollment.setJoinDate(joinDate);
        enrollment.setLeaveDate(leaveDate);
        enrollment.setStatus(status);
        return persist(enrollment);
    }

    private StudentGuardian createGuardian(Student student, Parent parent, Relationship relationship) {
        StudentGuardian guardian = new StudentGuardian();
        guardian.setStudent(student);
        guardian.setGuardian(parent);
        guardian.setRelationship(relationship);
        return persist(guardian);
    }

    private HomeroomAssignment createHomeroom(SchoolClass cls, Teacher teacher, AcademicYear year,
                                              LocalDate from, LocalDate to) {
        HomeroomAssignment assignment = new HomeroomAssignment();
        assignment.setCls(cls);
        assignment.setTeacher(teacher);
        assignment.setAcademicYear(year);
        assignment.setEffectiveFrom(from);
        assignment.setEffectiveTo(to);
        return persist(assignment);
    }

    private TeachingAssignment createAssignment(SchoolClass cls, Subject subject, Teacher teacher,
                                                AcademicYear year) {
        TeachingAssignment assignment = new TeachingAssignment();
        assignment.setCls(cls);
        assignment.setSubject(subject);
        assignment.setTeacher(teacher);
        assignment.setEffectiveFrom(year.getStartDate());
        assignment.setEffectiveTo(year.getEndDate());
        assignment.setStatus(AssignmentStatus.ACTIVE);
        return persist(assignment);
    }

    private Timetable createTimetable(SchoolClass cls, Semester semester, int version,
                                      TimetableStatus status, LocalDate from, LocalDate to) {
        Timetable timetable = new Timetable();
        timetable.setCls(cls);
        timetable.setSemester(semester);
        timetable.setVersion(version);
        timetable.setStatus(status);
        timetable.setEffectiveFrom(from);
        timetable.setEffectiveTo(to);
        return persist(timetable);
    }

    private void populateTimetable(Timetable timetable, List<TeachingAssignment> assignments,
                                   List<Period> periods, int firstDay, int lastDay,
                                   int firstPeriod, int lastPeriod) {
        int assignmentIndex = 0;
        for (int day = firstDay; day <= lastDay; day++) {
            for (int periodOrder = firstPeriod; periodOrder <= lastPeriod; periodOrder++) {
                Period period = periods.get(periodOrder - 1);
                TeachingAssignment assignment = assignments.get(assignmentIndex++ % assignments.size());
                Schedule slot = new Schedule();
                slot.setTimetable(timetable);
                slot.setAssignment(assignment);
                slot.setDayOfWeek(day);
                slot.setPeriod(periodOrder);
                slot.setPeriodRef(period);
                slot.setRoom(periodOrder <= 5 ? "D-A101" : "D-LAB1");
                slot.setShift(periodOrder <= 5 ? Shift.MORNING : Shift.AFTERNOON);
                timetable.getSlots().add(persist(slot));
            }
        }
    }

    private LeaveRequest createLeaveRequest(Student student, Parent parent, SchoolClass cls,
                                            AcademicYear year, Teacher approvedBy,
                                            LocalDate from, LocalDate to, LeaveShift shift,
                                            String reason, LeaveStatus status, String response,
                                            LocalDateTime approvedAt) {
        LeaveRequest request = new LeaveRequest();
        request.setStudent(student);
        request.setParent(parent);
        request.setCls(cls);
        request.setAcademicYear(year);
        request.setApprovedBy(approvedBy);
        request.setDateFrom(from);
        request.setDateTo(to);
        request.setShift(shift);
        request.setReason(reason);
        request.setStatus(status);
        request.setResponse(response);
        request.setApprovedAt(approvedAt);
        return persist(request);
    }

    private Attachment createAttachment(LeaveRequest leaveRequest, Message message, String url,
                                        String name, int size, String mimeType) {
        Attachment attachment = new Attachment();
        attachment.setLeaveRequest(leaveRequest);
        attachment.setMessage(message);
        attachment.setFileUrl(url);
        attachment.setFileName(name);
        attachment.setFileSize(size);
        attachment.setMimeType(mimeType);
        return persist(attachment);
    }

    private void createAttendanceHistory(List<Student> students, SchoolClass cls, Teacher teacher,
                                         List<LocalDate> days, Timetable timetable,
                                         LeaveRequest approvedLeave,
                                         LocalDate approvedLeaveDate) {
        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            LocalDate date = days.get(dayIndex);
            Schedule morningSchedule = scheduleFor(timetable, date, Shift.MORNING);
            Map<Student, AttendanceStatus> statuses = new LinkedHashMap<>();
            for (Student student : students) {
                AttendanceStatus status = AttendanceStatus.PRESENT;
                if (date.equals(approvedLeaveDate) && student == approvedLeave.getStudent()) {
                    status = AttendanceStatus.ABSENT_WITH_LEAVE;
                } else if (dayIndex == 0 && student == students.get(3)) {
                    status = AttendanceStatus.ABSENT_WITHOUT_LEAVE;
                }
                statuses.put(student, status);
            }
            createAttendanceSession(cls, teacher, date, Shift.MORNING, morningSchedule,
                statuses, approvedLeave, approvedLeaveDate, dayIndex < days.size() - 1);

            if (dayIndex >= days.size() - 2) {
                Schedule afternoonSchedule = scheduleFor(
                    timetable, date, Shift.AFTERNOON);
                Map<Student, AttendanceStatus> afternoonStatuses = new LinkedHashMap<>();
                for (Student student : students) {
                    afternoonStatuses.put(student, AttendanceStatus.PRESENT);
                }
                createAttendanceSession(cls, teacher, date, Shift.AFTERNOON, afternoonSchedule,
                    afternoonStatuses, null, null, dayIndex < days.size() - 1);
            }
        }
    }

    private Schedule scheduleFor(Timetable timetable, LocalDate date, Shift shift) {
        int dayOfWeek = date.getDayOfWeek().getValue() % 7 + 1;
        if (date.isBefore(timetable.getSemester().getStartDate())
                || date.isAfter(timetable.getSemester().getEndDate())
                || date.isBefore(timetable.getEffectiveFrom())
                || (timetable.getEffectiveTo() != null
                    && date.isAfter(timetable.getEffectiveTo()))) {
            throw new IllegalStateException(
                "Ngày điểm danh demo không thuộc thời khóa biểu hiệu lực: " + date);
        }
        return timetable.getSlots().stream()
            .filter(slot -> slot.getDayOfWeek() == dayOfWeek)
            .filter(slot -> slot.getShift() == shift)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Không có tiết học demo cho " + date + " - " + shift));
    }

    private void createAttendanceSession(SchoolClass cls, Teacher teacher, LocalDate date,
                                         Shift shift, Schedule schedule,
                                         Map<Student, AttendanceStatus> statuses,
                                         LeaveRequest approvedLeave, LocalDate approvedLeaveDate,
                                         boolean closed) {
        int absent = (int) statuses.values().stream()
            .filter(status -> status != AttendanceStatus.PRESENT).count();
        AttendanceSession session = new AttendanceSession();
        session.setCls(cls);
        session.setTeacher(teacher);
        session.setDate(date);
        session.setShift(shift);
        session.setSchedule(schedule);
        session.setTotal(statuses.size());
        session.setPresent(statuses.size() - absent);
        session.setAbsent(absent);
        session.setIsClosed(closed);
        persist(session);

        for (Map.Entry<Student, AttendanceStatus> entry : statuses.entrySet()) {
            AttendanceDetail detail = new AttendanceDetail();
            detail.setSession(session);
            detail.setStudent(entry.getKey());
            detail.setStatus(entry.getValue());
            if (entry.getValue() == AttendanceStatus.ABSENT_WITH_LEAVE) {
                detail.setNote("Nghỉ có phép - đã duyệt");
            } else if (entry.getValue() == AttendanceStatus.ABSENT_WITHOUT_LEAVE) {
                detail.setNote("Vắng chưa có đơn xin phép");
            }
            persist(detail);

            Attendance attendance = new Attendance();
            attendance.setStudent(entry.getKey());
            attendance.setCls(cls);
            attendance.setTeacher(teacher);
            attendance.setSchedule(schedule);
            attendance.setDate(date);
            attendance.setShift(shift);
            attendance.setStatus(entry.getValue());
            if (approvedLeave != null && date.equals(approvedLeaveDate)
                && entry.getKey() == approvedLeave.getStudent()) {
                attendance.setLeaveRequest(approvedLeave);
            }
            persist(attendance);
        }
    }

    private void createAttendanceCorrectionRequests(SchoolClass cls, Teacher teacher,
                                                    List<LocalDate> days, Student student) {
        AttendanceCorrectionRequest pending = new AttendanceCorrectionRequest();
        pending.setCls(cls);
        pending.setTeacher(teacher);
        pending.setDate(days.get(days.size() - 1));
        pending.setShift(Shift.MORNING);
        pending.setProposedEntries("[{\"studentId\":" + student.getId()
            + ",\"status\":\"PRESENT\",\"note\":\"Học sinh vào lớp sau khi kiểm tra lại\"}]");
        pending.setReason("Điều chỉnh sau khi giáo viên xác minh lại tình trạng có mặt");
        pending.setStatus(AttendanceCorrectionStatus.PENDING);
        persist(pending);

        AttendanceCorrectionRequest approved = new AttendanceCorrectionRequest();
        approved.setCls(cls);
        approved.setTeacher(teacher);
        approved.setDate(days.get(1));
        approved.setShift(Shift.AFTERNOON);
        approved.setProposedEntries("[{\"studentId\":" + student.getId()
            + ",\"status\":\"ABSENT_WITHOUT_LEAVE\"}]");
        approved.setReason("Cập nhật lại kết quả điểm danh theo biên bản lớp");
        approved.setStatus(AttendanceCorrectionStatus.APPROVED);
        approved.setReviewedAt(LocalDateTime.now(SCHOOL_ZONE).minusDays(1));
        persist(approved);
    }

    private GradeBook createGradeBook(SchoolClass cls, Subject subject, Semester semester,
                                      AcademicYearGradeConfig config, GradeBookStatus status,
                                      boolean finalized) {
        GradeBook gradeBook = new GradeBook();
        gradeBook.setCls(cls);
        gradeBook.setSubject(subject);
        gradeBook.setSemester(semester);
        gradeBook.setStatus(status);
        gradeBook.setIsFinalized(finalized);

        AcademicYearGradeConfigItem tx = configItem(config, "TX");
        AcademicYearGradeConfigItem mid = configItem(config, "GK");
        AcademicYearGradeConfigItem fin = configItem(config, "CK");
        addGradeItem(gradeBook, tx, "TX1", "Thường xuyên 1", 1, 0);
        addGradeItem(gradeBook, tx, "TX2", "Thường xuyên 2", 1, 1);
        addGradeItem(gradeBook, mid, "GK", "Giữa kỳ", 2, 2);
        addGradeItem(gradeBook, fin, "CK", "Cuối kỳ", 3, 3);
        return persist(gradeBook);
    }

    private AcademicYearGradeConfigItem configItem(AcademicYearGradeConfig config, String code) {
        return config.getItems().stream()
            .filter(item -> item.getCode().equals(code))
            .findFirst()
            .orElseThrow();
    }

    private void addGradeItem(GradeBook gradeBook, AcademicYearGradeConfigItem configItem,
                              String code, String name, int weight, int order) {
        GradeItem item = new GradeItem();
        item.setGradeBook(gradeBook);
        item.setConfigItem(configItem);
        item.setCode(code);
        item.setName(name);
        item.setWeight(weight);
        item.setMaxScore(10);
        item.setOrder(order);
        item.setEntryRole(configItem.getEntryRole());
        item.setAssessmentType(configItem.getAssessmentType());
        item.setRequiredEntry(configItem.getRequiredEntry());
        gradeBook.getItems().add(item);
    }

    private List<StudentScore> seedScores(GradeBook gradeBook, List<Student> students,
                                          User teacherUser, User admin, int gradedItemCount) {
        List<StudentScore> scores = new ArrayList<>();
        for (int studentIndex = 0; studentIndex < students.size(); studentIndex++) {
            for (int itemIndex = 0; itemIndex < gradeBook.getItems().size(); itemIndex++) {
                GradeItem item = gradeBook.getItems().get(itemIndex);
                StudentScore studentScore = new StudentScore();
                studentScore.setGradeItem(item);
                studentScore.setStudent(students.get(studentIndex));
                boolean graded = itemIndex < gradedItemCount;
                studentScore.setIsGraded(graded);
                studentScore.setIsCommentBased(false);
                if (graded) {
                    BigDecimal score = BigDecimal.valueOf(7.2 + ((studentIndex * 3 + itemIndex) % 7) * 0.35)
                        .setScale(2, RoundingMode.HALF_UP);
                    studentScore.setScore(score);
                    studentScore.setEnteredBy(item.getEntryRole() == GradeEntryRole.SUBJECT_TEACHER
                        ? teacherUser : admin);
                    if (gradeBook.getStatus() == GradeBookStatus.PUBLISHED
                        || gradeBook.getStatus() == GradeBookStatus.LOCKED) {
                        studentScore.setPublishedAt(LocalDateTime.now(SCHOOL_ZONE).minusDays(1));
                    }
                } else {
                    studentScore.setNote("Chưa nhập điểm");
                }
                scores.add(persist(studentScore));
            }
        }
        return scores;
    }

    private StudentScoreAudit createScoreAudit(StudentScore score, BigDecimal oldScore,
                                               BigDecimal newScore, User changedBy, String reason) {
        StudentScoreAudit audit = new StudentScoreAudit();
        audit.setStudentScore(score);
        audit.setOldScore(oldScore);
        audit.setNewScore(newScore);
        audit.setChangedBy(changedBy);
        audit.setReason(reason);
        audit.setChangedAt(LocalDateTime.now(SCHOOL_ZONE).minusHours(12));
        return persist(audit);
    }

    private void createSemesterResults(List<Student> students, SchoolClass cls, Semester semester,
                                       BigDecimal baseGpa) {
        for (int index = 0; index < students.size(); index++) {
            SemesterResult result = new SemesterResult();
            result.setStudent(students.get(index));
            result.setCls(cls);
            result.setSemester(semester);
            BigDecimal gpa = baseGpa.add(BigDecimal.valueOf((students.size() - index) * 0.12))
                .setScale(2, RoundingMode.HALF_UP);
            result.setGpa(gpa);
            result.setRank(index + 1);
            result.setHonor(index == 0 ? "Học sinh xuất sắc" : index <= 2 ? "Học sinh giỏi" : null);
            result.setConduct(index == students.size() - 1 ? "Khá" : "Tốt");
            result.setAcademicAbility(gpa.compareTo(new BigDecimal("8.00")) >= 0 ? "Giỏi" : "Khá");
            persist(result);
        }
    }

    private Map<String, TuitionBill> createFinanceData(Semester semester,
                                                       SchoolClass class12A1,
                                                       SchoolClass class12A2,
                                                       SchoolClass class10A1,
                                                       List<Student> students12A1,
                                                       List<Student> students12A2,
                                                       Student studentNam) {
        FeeCategory tuition = createFeeCategory("[DEMO] Học phí", "Học phí chính khóa");
        FeeCategory meal = createFeeCategory("[DEMO] Tiền ăn", "Suất ăn bán trú");
        FeeCategory transport = createFeeCategory("[DEMO] Xe buýt", "Dịch vụ xe đưa đón");

        FeeTemplate tuition12A1 = createFeeTemplate(tuition, class12A1, semester,
            "Học phí học kỳ I", new BigDecimal("12500000"), LocalDate.of(2026, 8, 15));
        FeeTemplate meal12A1 = createFeeTemplate(meal, class12A1, semester,
            "Tiền ăn tháng 7", new BigDecimal("1200000"), LocalDate.of(2026, 7, 20));
        FeeTemplate bus12A1 = createFeeTemplate(transport, class12A1, semester,
            "Xe buýt học kỳ I", new BigDecimal("3200000"), LocalDate.of(2026, 7, 25));
        FeeTemplate tuition12A2 = createFeeTemplate(tuition, class12A2, semester,
            "Học phí học kỳ I", new BigDecimal("12500000"), LocalDate.of(2026, 8, 15));
        FeeTemplate tuition10A1 = createFeeTemplate(tuition, class10A1, semester,
            "Học phí học kỳ I", new BigDecimal("11800000"), LocalDate.of(2026, 8, 15));
        FeeTemplate meal10A1 = createFeeTemplate(meal, class10A1, semester,
            "Tiền ăn tháng 7", new BigDecimal("1200000"), LocalDate.of(2026, 7, 20));

        Map<String, TuitionBill> highlighted = new LinkedHashMap<>();
        for (int index = 0; index < students12A1.size(); index++) {
            Student student = students12A1.get(index);
            BillStatus tuitionStatus = index == 0 ? BillStatus.PAID
                : index == 1 ? BillStatus.PROCESSING : BillStatus.UNPAID;
            TuitionBill tuitionBill = createBill(student, class12A1, semester, tuition12A1, tuitionStatus);
            if (tuitionStatus == BillStatus.PAID) {
                createTransaction(tuitionBill, PaymentStatus.SUCCESS, "BANK_TRANSFER", "DEMO-PAID-" + index);
            } else if (tuitionStatus == BillStatus.PROCESSING) {
                createTransaction(tuitionBill, PaymentStatus.PENDING, "VNPAY", "DEMO-PENDING-" + index);
            }
            TuitionBill mealBill = createBill(student, class12A1, semester, meal12A1,
                index % 2 == 0 ? BillStatus.UNPAID : BillStatus.PAID);
            if (mealBill.getStatus() == BillStatus.PAID) {
                createTransaction(mealBill, PaymentStatus.SUCCESS, "BANK_TRANSFER", "DEMO-MEAL-" + index);
            }
            TuitionBill busBill = createBill(student, class12A1, semester, bus12A1,
                index == 0 ? BillStatus.PROCESSING : BillStatus.UNPAID);
            if (index == 0) {
                createTransaction(busBill, PaymentStatus.PENDING, "VNPAY", "DEMO-BUS-PENDING");
                highlighted.put("PROCESSING", busBill);
                highlighted.put("PAID", tuitionBill);
                highlighted.put("UNPAID", mealBill);
            } else if (index == 2) {
                createTransaction(busBill, PaymentStatus.FAILED, "VNPAY", "DEMO-BUS-FAILED");
            }
        }
        for (Student student : students12A2) {
            createBill(student, class12A2, semester, tuition12A2, BillStatus.UNPAID);
        }
        TuitionBill namTuition = createBill(studentNam, class10A1, semester, tuition10A1, BillStatus.UNPAID);
        createBill(studentNam, class10A1, semester, meal10A1, BillStatus.PAID);
        highlighted.put("SECOND_CHILD", namTuition);
        return highlighted;
    }

    private FeeCategory createFeeCategory(String name, String description) {
        FeeCategory category = new FeeCategory();
        category.setName(name);
        category.setDescription(description);
        return persist(category);
    }

    private FeeTemplate createFeeTemplate(FeeCategory category, SchoolClass cls, Semester semester,
                                          String name, BigDecimal amount, LocalDate dueDate) {
        FeeTemplate template = new FeeTemplate();
        template.setFeeCategory(category);
        template.setCls(cls);
        template.setSemester(semester);
        template.setName(name);
        template.setAmount(amount);
        template.setDueDate(dueDate);
        return persist(template);
    }

    private TuitionBill createBill(Student student, SchoolClass cls, Semester semester,
                                   FeeTemplate template, BillStatus status) {
        TuitionBill bill = new TuitionBill();
        bill.setStudent(student);
        bill.setCls(cls);
        bill.setSemester(semester);
        bill.setFeeTemplate(template);
        bill.setName(template.getName());
        bill.setAmount(template.getAmount());
        bill.setDueDate(template.getDueDate());
        bill.setStatus(status);
        if (status == BillStatus.PAID) {
            bill.setPaidAt(LocalDateTime.now(SCHOOL_ZONE).minusDays(3));
        }
        return persist(bill);
    }

    private PaymentTransaction createTransaction(TuitionBill bill, PaymentStatus status,
                                                 String method, String reference) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTuitionBill(bill);
        transaction.setAmount(bill.getAmount());
        transaction.setPaymentMethod(method);
        transaction.setTransactionRef(reference);
        transaction.setStatus(status);
        if (status == PaymentStatus.SUCCESS) {
            transaction.setPaidAt(bill.getPaidAt());
        }
        return persist(transaction);
    }

    private void createAnnouncementPolicy(AcademicYear year, User admin) {
        AnnouncementPolicySetting setting = new AnnouncementPolicySetting();
        setting.setAcademicYear(year);
        setting.setEnabled(true);
        setting.setRejectionMessage(
            "Thông báo này đã vi phạm câu từ trong chính sách của nhà trường.");
        setting.setUpdatedBy(admin);
        persist(setting);

        createAnnouncementRule(year, admin, "mua ngay");
        createAnnouncementRule(year, admin, "thông báo nháp");
    }

    private void createAnnouncementRule(AcademicYear year, User admin, String phrase) {
        AnnouncementContentRule rule = new AnnouncementContentRule();
        rule.setAcademicYear(year);
        rule.setPhrase(phrase);
        rule.setNormalizedPhrase(phrase);
        rule.setScope(AnnouncementPolicyScope.ALL);
        rule.setMatchType(AnnouncementPolicyMatchType.CONTAINS);
        rule.setUpdatedBy(admin);
        persist(rule);
    }

    private Map<String, Announcement> createAnnouncements(AcademicYear year, User admin,
                                                           Teacher teacherMath,
                                                           Teacher teacherLiterature,
                                                           SchoolClass class12A1,
                                                           SchoolClass class12A2,
                                                           SchoolClass class10A1) {
        Map<String, Announcement> announcements = new LinkedHashMap<>();
        Announcement meeting = createAnnouncement(
            year, teacherMath, teacherMath.getUser(), AnnouncementDeliveryStatus.PUBLISHED, null,
            "HOMEROOM_TEACHER", "CLASSES",
            "Họp phụ huynh đầu học kỳ",
            "Kính mời phụ huynh tham dự cuộc họp tại phòng D-A101 lúc 08:00 thứ Bảy.",
            TargetRole.PARENT, List.of(class12A1)
        );
        announcements.put("MEETING", meeting);
        Announcement exam = createAnnouncement(
            year, teacherMath, teacherMath.getUser(), AnnouncementDeliveryStatus.PUBLISHED, null,
            "SUBJECT_TEACHER", "CLASSES",
            "Lịch kiểm tra Toán chương 1",
            "Bài kiểm tra 45 phút diễn ra vào tiết 2 thứ Tư. Học sinh chuẩn bị máy tính cầm tay.",
            TargetRole.STUDENT, List.of(class12A1, class12A2)
        );
        announcements.put("EXAM", exam);
        Announcement readingActivity = createAnnouncement(
            year, teacherLiterature, teacherLiterature.getUser(), AnnouncementDeliveryStatus.PUBLISHED, null,
            "SUBJECT_TEACHER", "CLASSES",
            "Đề xuất hoạt động đọc sách",
            "Kế hoạch đọc sách theo nhóm dành cho học sinh khối 12.",
            TargetRole.ALL, List.of(class12A1, class12A2)
        );
        announcements.put("READING_ACTIVITY", readingActivity);
        Announcement rejected = createAnnouncement(
            year, teacherLiterature, teacherLiterature.getUser(), AnnouncementDeliveryStatus.SYSTEM_REJECTED,
            "Thông báo này đã vi phạm câu từ trong chính sách của nhà trường.",
            "SUBJECT_TEACHER", "CLASSES",
            "Hoạt động ngoại khóa dự kiến",
            "Thông báo nháp để kiểm thử trạng thái từ chối.",
            TargetRole.PARENT, List.of(class10A1)
        );
        announcements.put("SYSTEM_REJECTED", rejected);
        Announcement schoolWide = createAnnouncement(
            year, null, admin, AnnouncementDeliveryStatus.PUBLISHED, null,
            "ADMIN", "SCHOOL",
            "Bảo trì hệ thống MyFschool",
            "Hệ thống tạm ngừng 30 phút từ 22:00 để nâng cấp định kỳ.",
            TargetRole.ALL, List.of()
        );
        announcements.put("SCHOOL", schoolWide);
        Announcement schoolPolicy = createAnnouncement(
            year, null, admin, AnnouncementDeliveryStatus.PUBLISHED, null,
            "ADMIN", "SCHOOL",
            "Cập nhật quy định sử dụng MyFschool",
            "Nhà trường cập nhật hướng dẫn bảo mật tài khoản dành cho toàn bộ phụ huynh, học sinh và giáo viên.",
            TargetRole.ALL, List.of()
        );
        announcements.put("SCHOOL_POLICY", schoolPolicy);
        userRepository.findAll().stream()
            .filter(user -> user.getRole() != UserRole.ADMIN)
            .forEach(user -> {
                createAnnouncementRead(schoolWide, user, null);
                createAnnouncementRead(schoolPolicy, user, null);
            });
        return announcements;
    }

    private Announcement createAnnouncement(AcademicYear year, Teacher teacher, User sender,
                                            AnnouncementDeliveryStatus deliveryStatus,
                                            String systemRejectionMessage,
                                            String senderType, String recipientScope,
                                            String title, String body, TargetRole targetRole,
                                            List<SchoolClass> classes) {
        Announcement announcement = new Announcement();
        announcement.setAcademicYear(year);
        announcement.setTeacher(teacher);
        announcement.setSender(sender);
        announcement.setDeliveryStatus(deliveryStatus);
        announcement.setSystemRejectionMessage(systemRejectionMessage);
        announcement.setSenderType(senderType);
        announcement.setRecipientScope(recipientScope);
        announcement.setTitle(title);
        announcement.setBody(body);
        announcement.setTargetRole(targetRole);
        persist(announcement);
        for (SchoolClass cls : classes) {
            AnnouncementClass link = new AnnouncementClass();
            link.setAnnouncement(announcement);
            link.setCls(cls);
            announcement.getAnnouncementClasses().add(persist(link));
        }
        return announcement;
    }

    private AnnouncementRead createAnnouncementRead(Announcement announcement, User user,
                                                    LocalDateTime readAt) {
        AnnouncementRead read = new AnnouncementRead();
        read.setAnnouncement(announcement);
        read.setUser(user);
        read.setRecipientRole(user.getRole());
        read.setUserName(user.getName());
        read.setReadAt(readAt);
        return persist(read);
    }

    private void createCommunicationData(User teacherMath, User teacherLiterature,
                                         User parent, User studentAn, User studentChi) {
        Conversation parentTeacher = createConversation(List.of(parent, teacherMath));
        Message message1 = createMessage(parentTeacher, teacherMath, 1,
            "Chào phụ huynh, tuần này An học tập tốt và tích cực phát biểu.", "pt-1");
        createReceipt(message1, parent, MessageReceiptStatus.READ, true);
        Message message2 = createMessage(parentTeacher, parent, 2,
            "Cảm ơn cô. Gia đình sẽ tiếp tục nhắc An hoàn thành bài tập đúng hạn.", "pt-2");
        createReceipt(message2, teacherMath, MessageReceiptStatus.DELIVERED, false);
        Message message3 = createMessage(parentTeacher, teacherMath, 3,
            "Cô gửi phụ huynh kế hoạch ôn tập chương 1.", "pt-3");
        createReceipt(message3, parent, MessageReceiptStatus.DELIVERED, false);
        createAttachment(null, message3, "/demo/attachments/ke-hoach-on-tap.pdf",
            "ke-hoach-on-tap.pdf", 245_760, "application/pdf");
        parentTeacher.setLastMessage(message3.getContent());
        parentTeacher.setLastMessageAt(LocalDateTime.now(SCHOOL_ZONE).minusMinutes(20));

        Conversation studentTeacher = createConversation(List.of(studentAn, teacherMath));
        Message studentQuestion = createMessage(studentTeacher, studentAn, 1,
            "Cô ơi, bài số 5 em có thể dùng cách giải thứ hai không ạ?", "st-1");
        createReceipt(studentQuestion, teacherMath, MessageReceiptStatus.READ, true);
        Message teacherAnswer = createMessage(studentTeacher, teacherMath, 2,
            "Được em nhé, miễn là trình bày đầy đủ các bước suy luận.", "st-2");
        createReceipt(teacherAnswer, studentAn, MessageReceiptStatus.READ, true);
        studentTeacher.setLastMessage(teacherAnswer.getContent());
        studentTeacher.setLastMessageAt(LocalDateTime.now(SCHOOL_ZONE).minusHours(2));

        Conversation support = createConversation(List.of(studentChi, teacherLiterature));
        Message supportMessage = createMessage(support, teacherLiterature, 1,
            "Chi nhớ nộp bản sửa bài viết trước 20:00 tối nay nhé.", "support-1");
        createReceipt(supportMessage, studentChi, MessageReceiptStatus.DELIVERED, false);
        support.setLastMessage(supportMessage.getContent());
        support.setLastMessageAt(LocalDateTime.now(SCHOOL_ZONE).minusMinutes(5));
    }

    private Conversation createConversation(List<User> users) {
        Conversation conversation = persist(new Conversation());
        for (User user : users) {
            ConversationParticipant participant = new ConversationParticipant();
            participant.setConversation(conversation);
            participant.setUser(user);
            participant.setJoinedAt(LocalDateTime.now(SCHOOL_ZONE).minusDays(10));
            participant.setLastSeenAt(LocalDateTime.now(SCHOOL_ZONE).minusHours(1));
            conversation.getParticipants().add(persist(participant));
        }
        return conversation;
    }

    private Message createMessage(Conversation conversation, User sender, long sequence,
                                  String content, String clientMessageId) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        message.setClientMessageId("demo-" + clientMessageId);
        message.setMessageType(MessageType.TEXT);
        message.setServerSeq(sequence);
        conversation.getMessages().add(persist(message));
        return message;
    }

    private MessageReceipt createReceipt(Message message, User user, MessageReceiptStatus status,
                                         boolean read) {
        MessageReceipt receipt = new MessageReceipt();
        receipt.setMessage(message);
        receipt.setUser(user);
        receipt.setStatus(status);
        receipt.setDeliveredAt(LocalDateTime.now(SCHOOL_ZONE).minusHours(1));
        if (read) {
            receipt.setReadAt(LocalDateTime.now(SCHOOL_ZONE).minusMinutes(45));
        }
        return persist(receipt);
    }

    private void createNotifications(User admin, User teacher, User parent, User student,
                                     Map<String, TuitionBill> bills,
                                     LeaveRequest approvedLeave,
                                     Map<String, Announcement> announcements) {
        createNotification(parent, bills.get("UNPAID"), "Học phí sắp đến hạn",
            "Khoản tiền ăn tháng 7 sẽ đến hạn thanh toán.", "TUITION",
            bills.get("UNPAID").getId(), "TUITION_BILL", false);
        createNotification(parent, bills.get("PAID"), "Thanh toán thành công",
            "Hệ thống đã ghi nhận học phí học kỳ I.", "PAYMENT",
            bills.get("PAID").getId(), "TUITION_BILL", true);
        createNotification(parent, null, "Đơn xin nghỉ đã được duyệt",
            "Đơn xin nghỉ của Nguyễn Minh An đã được giáo viên xác nhận.", "LEAVE",
            approvedLeave.getId(), "LEAVE_REQUEST", false);
        createNotification(parent, bills.get("SECOND_CHILD"), "Khoản thu của Nguyễn Hoài Nam",
            "Học phí học kỳ I của học sinh Nguyễn Hoài Nam đã được phát hành.", "TUITION",
            bills.get("SECOND_CHILD").getId(), "TUITION_BILL", false);

        createNotification(student, null, "Điểm Toán đã được công bố",
            "Bảng điểm Toán học kỳ I đã có cập nhật mới.", "GRADE", null, "GRADE_BOOK", false);
        createNotification(student, null, "Lịch kiểm tra mới",
            announcements.get("EXAM").getTitle(), "ANNOUNCEMENT",
            announcements.get("EXAM").getId(), "ANNOUNCEMENT", false);
        createNotification(student, null, "Tin nhắn từ giáo viên",
            "Bạn có một phản hồi mới trong cuộc trò chuyện.", "MESSAGE", null, "CONVERSATION", true);

        createNotification(teacher, null, "Có đơn xin nghỉ chờ xử lý",
            "Lớp chủ nhiệm có đơn xin nghỉ mới.", "LEAVE", null, "LEAVE_REQUEST", false);
        createNotification(teacher, null, "Yêu cầu sửa điểm danh",
            "Yêu cầu điều chỉnh điểm danh đang chờ quản trị viên duyệt.", "ATTENDANCE", null,
            "ATTENDANCE_CORRECTION", false);

        createNotification(admin, null, "Năm học nháp chưa hoàn tất",
            DRAFT_YEAR_NAME + " còn thiếu cấu hình bắt buộc.", "ACADEMIC_YEAR", null,
            "ACADEMIC_YEAR", true);
    }

    private Notification createNotification(User user, TuitionBill bill, String title, String body,
                                            String tag, Long relatedId, String relatedType,
                                            boolean read) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTuitionBill(bill);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setTag(tag);
        notification.setRelatedId(relatedId);
        notification.setRelatedType(relatedType);
        notification.setIsRead(read);
        return persist(notification);
    }

    private void createAuditLogs(User admin) {
        createAuditLog(admin, "POST", "/api/admin/announcements", 200, 87L, null);
        createAuditLog(admin, "PUT", "/api/admin/academic-years/demo/activate", 409, 42L,
            "Năm học chưa hoàn tất cấu hình bắt buộc");
        createAuditLog(admin, "GET", "/api/admin/users?role=TEACHER", 200, 31L, null);
    }

    private AuditLog createAuditLog(User user, String method, String uri, int status,
                                    long executionMs, String error) {
        AuditLog audit = new AuditLog();
        audit.setUserId(user.getId());
        audit.setUsername(user.getName());
        audit.setRole(user.getRole().name());
        audit.setHttpMethod(method);
        audit.setUri(uri);
        audit.setClientIp("127.0.0.1");
        audit.setStatusCode(status);
        audit.setExecutionTimeMs(executionMs);
        audit.setErrorMessage(error);
        return persist(audit);
    }

    private List<LocalDate> recentSchoolDays(LocalDate today, int count) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate cursor = today;
        while (days.size() < count) {
            if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY
                    && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                days.add(cursor);
            }
            cursor = cursor.minusDays(1);
        }
        Collections.reverse(days);
        return days;
    }

    private LocalDate nextSchoolDay(LocalDate date) {
        LocalDate candidate = date.plusDays(1);
        while (candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
