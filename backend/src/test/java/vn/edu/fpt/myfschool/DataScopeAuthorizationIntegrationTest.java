package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.Enrollment;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.SemesterResult;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.SemesterResultRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DataScopeAuthorizationIntegrationTest extends BaseIntegrationTest {

    @Autowired SemesterResultRepository semesterResultRepository;

    @Test
    void subjectTeacher_cannotReadWholeClassSemesterResults() throws Exception {
        saveResult(testStudent1, testClass, testSemester, 1);
        String teacherToken = loginAsTeacher();

        mockMvc.perform(get("/api/semester-results")
                .header("Authorization", authHeader(teacherToken))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/semester-results/ranking")
                .header("Authorization", authHeader(teacherToken))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    void homeroomTeacher_canReadAssignedClassSemesterResults() throws Exception {
        assignHomeroom(testClass, testAcademicYear, testSemester);
        saveResult(testStudent1, testClass, testSemester, 1);
        String teacherToken = loginAsTeacher();

        mockMvc.perform(get("/api/semester-results")
                .header("Authorization", authHeader(teacherToken))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.studentId").value(testStudent1.getId().intValue()))
            .andExpect(jsonPath("$.data.classId").value(testClass.getId().intValue()));

        mockMvc.perform(get("/api/semester-results/ranking")
                .header("Authorization", authHeader(teacherToken))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.rankings[0].studentId")
                .value(testStudent1.getId().intValue()));
    }

    @Test
    void studentAndParent_canOnlyReadTheirOwnLinkedStudentResults() throws Exception {
        saveResult(testStudent1, testClass, testSemester, 1);

        mockMvc.perform(get("/api/semester-results")
                .header("Authorization", authHeader(loginAsStudent1()))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/semester-results")
                .header("Authorization", authHeader(loginAsStudent2()))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/semester-results")
                .header("Authorization", authHeader(loginAsParent()))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk());

        String outsiderToken = createOutsiderParent();
        mockMvc.perform(get("/api/semester-results")
                .header("Authorization", authHeader(outsiderToken))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    void classRoster_isLimitedToAssignedTeachersAndNeverExposedToParentsOrStudents()
            throws Exception {
        SchoolClass unassignedClass = createClass(testAcademicYear, "12B");
        String teacherToken = loginAsTeacher();

        mockMvc.perform(get("/api/classes/{id}/students", testClass.getId())
                .header("Authorization", authHeader(teacherToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(3));

        mockMvc.perform(get("/api/classes/{id}/students", unassignedClass.getId())
                .header("Authorization", authHeader(teacherToken)))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/classes/{id}/students", testClass.getId())
                .header("Authorization", authHeader(loginAsParent())))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/classes/{id}", testClass.getId())
                .header("Authorization", authHeader(loginAsStudent1())))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/classes")
                .header("Authorization", authHeader(loginAsStudent1()))
                .param("academicYearId", testAcademicYear.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[*].id",
                hasItem(testClass.getId().intValue())))
            .andExpect(jsonPath("$.data.content[*].id",
                not(hasItem(unassignedClass.getId().intValue()))));

        mockMvc.perform(get("/api/classes")
                .header("Authorization", authHeader(loginAsParent()))
                .param("academicYearId", testAcademicYear.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[*].id",
                hasItem(testClass.getId().intValue())))
            .andExpect(jsonPath("$.data.content[*].id",
                not(hasItem(unassignedClass.getId().intValue()))));
    }

    @Test
    void classAndSemesterScopes_doNotLeakAcrossAcademicYears() throws Exception {
        assignHomeroom(testClass, testAcademicYear, testSemester);
        YearScope other = createOtherYearScope();
        enroll(testStudent1, other.schoolClass(), other.year());
        saveResult(testStudent1, other.schoolClass(), other.semester(), 1);
        String teacherToken = loginAsTeacher();

        mockMvc.perform(get("/api/classes")
                .header("Authorization", authHeader(teacherToken))
                .param("academicYearId", testAcademicYear.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[*].id", hasItem(testClass.getId().intValue())));

        mockMvc.perform(get("/api/classes")
                .header("Authorization", authHeader(teacherToken))
                .param("academicYearId", other.year().getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(0));

        mockMvc.perform(get("/api/semester-results/ranking")
                .header("Authorization", authHeader(teacherToken))
                .param("classId", other.schoolClass().getId().toString())
                .param("semesterId", other.semester().getId().toString()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/semester-results")
                .header("Authorization", authHeader(teacherToken))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", other.semester().getId().toString()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/classes")
                .header("Authorization", authHeader(loginAsAdmin()))
                .param("academicYearId", other.year().getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[*].id",
                hasItem(other.schoolClass().getId().intValue())))
            .andExpect(jsonPath("$.data.content[*].id",
                not(hasItem(testClass.getId().intValue()))));
    }

    private void assignHomeroom(
            SchoolClass schoolClass, AcademicYear year, Semester semester) {
        HomeroomAssignment assignment = new HomeroomAssignment();
        assignment.setCls(schoolClass);
        assignment.setTeacher(testTeacher);
        assignment.setAcademicYear(year);
        assignment.setEffectiveFrom(semester.getStartDate());
        assignment.setEffectiveTo(semester.getEndDate());
        homeroomAssignmentRepository.save(assignment);
    }

    private void saveResult(
            vn.edu.fpt.myfschool.entity.Student student,
            SchoolClass schoolClass, Semester semester, int rank) {
        SemesterResult result = new SemesterResult();
        result.setStudent(student);
        result.setCls(schoolClass);
        result.setSemester(semester);
        result.setGpa(new BigDecimal("8.50"));
        result.setRank(rank);
        result.setHonor("Giỏi");
        result.setConduct("Tốt");
        result.setAcademicAbility("Giỏi");
        semesterResultRepository.save(result);
    }

    private String createOutsiderParent() throws Exception {
        User user = new User();
        user.setPhone("0911000001");
        user.setPassword(passwordEncoder.encode("test1234"));
        user.setName("PH Không Liên Kết");
        user.setRole(UserRole.PARENT);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        Parent parent = new Parent();
        parent.setUser(user);
        parentRepository.save(parent);
        return login(user.getPhone(), "test1234");
    }

    private YearScope createOtherYearScope() {
        AcademicYear year = new AcademicYear();
        year.setName("2027-2028");
        year.setStartDate(LocalDate.of(2027, 8, 1));
        year.setEndDate(LocalDate.of(2028, 5, 31));
        year.setStatus(AcademicYearStatus.DRAFT);
        year = academicYearRepository.save(year);

        SchoolClass schoolClass = createClass(year, "11Z");

        Semester semester = new Semester();
        semester.setName("HK I");
        semester.setOrder(1);
        semester.setStartDate(LocalDate.of(2027, 9, 1));
        semester.setEndDate(LocalDate.of(2028, 1, 15));
        semester.setIsCurrent(false);
        semester.setAcademicYear(year);
        semester = semesterRepository.save(semester);
        return new YearScope(year, schoolClass, semester);
    }

    private SchoolClass createClass(AcademicYear year, String name) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName(name);
        schoolClass.setGradeLevel(12);
        schoolClass.setAcademicYear(year);
        schoolClass.setSchoolName("FPT Schools");
        return classRepository.save(schoolClass);
    }

    private void enroll(
            vn.edu.fpt.myfschool.entity.Student student,
            SchoolClass schoolClass, AcademicYear year) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCls(schoolClass);
        enrollment.setAcademicYear(year);
        enrollment.setJoinDate(year.getStartDate());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollmentRepository.save(enrollment);
    }

    private record YearScope(
        AcademicYear year, SchoolClass schoolClass, Semester semester) {}
}
