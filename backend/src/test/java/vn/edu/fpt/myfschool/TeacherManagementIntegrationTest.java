package vn.edu.fpt.myfschool;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Subject;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.TeachingAssignment;
import vn.edu.fpt.myfschool.entity.User;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherManagementIntegrationTest extends BaseIntegrationTest {

    @Test
    void createTeacher_generatesSecureTemporaryPasswordWithoutExposingIt() throws Exception {
        String token = loginAsAdmin();
        MvcResult result = mockMvc.perform(post("/api/admin/users/teachers")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0911000001","name":"Giáo viên mới","email":"new.teacher@school.test","subjectIds":[%d]}
                    """.formatted(testSubject.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.teacher.employeeCode").isNotEmpty())
            .andExpect(jsonPath("$.data.credentialsEmailed").value(true))
            .andExpect(jsonPath("$.data.temporaryPassword").doesNotExist())
            .andReturn();

        User savedUser = userRepository.findByPhone("0911000001").orElseThrow();
        assertFalse(passwordEncoder.matches("12345678", savedUser.getPassword()));
        assertTrue(savedUser.getMustChangePassword());
        assertTrue(savedUser.getEmailVerifiedAt() != null);
        assertEquals("new.teacher@school.test", savedUser.getEmail());

        mockMvc.perform(post("/api/admin/users/teachers")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0911000002","name":"Trùng email","email":"NEW.TEACHER@SCHOOL.TEST","subjectIds":[%d]}
                    """.formatted(testSubject.getId())))
            .andExpect(status().isConflict());
    }

    @Test
    void adminCanEditLockAndUnlockTeacherAccountButCannotResetPasswordManually() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(put("/api/admin/users/teachers/{id}", testTeacher.getId())
                .header("Authorization", authHeader(token))
                .param("academicYearId", testAcademicYear.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0911222333","name":"Giáo viên đã sửa","email":"edited.teacher@school.test"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Giáo viên đã sửa"))
            .andExpect(jsonPath("$.data.phone").value("0911222333"));

        mockMvc.perform(put("/api/admin/users/teachers/{id}", testTeacher.getId())
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"0909000009","name":"Trùng tài khoản","email":"another@school.test"}
                    """))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0911222333\",\"password\":\"test1234\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/users/teachers/{id}/reset-password", testTeacher.getId())
                .header("Authorization", authHeader(token)))
            .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/admin/users/{id}/status", testTeacher.getUser().getId())
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"LOCKED\"}"))
            .andExpect(status().isOk());
        assertEquals(UserStatus.LOCKED, userRepository.findById(testTeacher.getUser().getId()).orElseThrow().getStatus());

        mockMvc.perform(put("/api/admin/users/{id}/status", testTeacher.getUser().getId())
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\"}"))
            .andExpect(status().isOk());
        assertEquals(UserStatus.ACTIVE, userRepository.findById(testTeacher.getUser().getId()).orElseThrow().getStatus());
    }

    @Test
    void teacherListAndSummary_areStrictlyIsolatedByAcademicYear() throws Exception {
        String token = loginAsAdmin();
        Subject literature = subjectRepository.findByCode("VAN12").orElseThrow();
        AcademicYear secondYear = createAcademicYear("2027-2028", LocalDate.of(2027, 8, 1), LocalDate.of(2028, 5, 31));
        SchoolClass secondYearClass = createClass("11B", secondYear, 11);
        Teacher secondTeacher = createTeacher("0911000002", "GV200", "Giáo viên năm sau", literature);

        createTeachingAssignment(testTeacher, testSubject, secondYearClass, secondYear.getStartDate());
        createTeachingAssignment(secondTeacher, literature, secondYearClass, secondYear.getStartDate());
        createHomeroom(testTeacher, testClass, testAcademicYear);
        createHomeroom(secondTeacher, secondYearClass, secondYear);

        JsonNode firstYearData = getTeacherPage(token, testAcademicYear.getId(), 20);
        JsonNode firstYearTeacher = findTeacher(firstYearData, testTeacher.getId());
        JsonNode unassignedInFirstYear = findTeacher(firstYearData, secondTeacher.getId());
        assertEquals(1, firstYearTeacher.path("teachingAssignments").size());
        assertEquals("12A", firstYearTeacher.path("teachingAssignments").get(0).path("className").asText());
        assertEquals(1, firstYearTeacher.path("homeroomClasses").size());
        assertEquals("12A", firstYearTeacher.path("homeroomClasses").get(0).path("className").asText());
        assertEquals(0, unassignedInFirstYear.path("teachingAssignments").size());
        assertEquals(0, unassignedInFirstYear.path("homeroomClasses").size());

        JsonNode secondYearData = getTeacherPage(token, secondYear.getId(), 20);
        JsonNode teacherInSecondYear = findTeacher(secondYearData, testTeacher.getId());
        JsonNode homeroomTeacherInSecondYear = findTeacher(secondYearData, secondTeacher.getId());
        assertEquals(1, teacherInSecondYear.path("teachingAssignments").size());
        assertEquals("11B", teacherInSecondYear.path("teachingAssignments").get(0).path("className").asText());
        assertEquals(0, teacherInSecondYear.path("homeroomClasses").size());
        assertEquals("11B", homeroomTeacherInSecondYear.path("homeroomClasses").get(0).path("className").asText());

        JsonNode firstSummary = getSummary(token, testAcademicYear.getId());
        JsonNode secondSummary = getSummary(token, secondYear.getId());
        assertEquals(1, firstSummary.path("unassigned").asInt());
        assertEquals(0, secondSummary.path("unassigned").asInt());
        assertEquals(1, firstSummary.path("homeroom").asInt());
        assertEquals(1, secondSummary.path("homeroom").asInt());
    }

    @Test
    void teacherList_usesRealPaginationAndSearch() throws Exception {
        String token = loginAsAdmin();
        for (int index = 0; index < 25; index++) {
            createTeacher("0922%06d".formatted(index), "GV-P%03d".formatted(index), "Giáo viên phân trang " + index, testSubject);
        }

        mockMvc.perform(get("/api/admin/users/teachers")
                .header("Authorization", authHeader(token))
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(20))
            .andExpect(jsonPath("$.data.totalElements").value(26))
            .andExpect(jsonPath("$.data.totalPages").value(2));

        mockMvc.perform(get("/api/admin/users/teachers")
                .header("Authorization", authHeader(token))
                .param("keyword", "GV-P024")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].employeeCode").value("GV-P024"));
    }

    @Test
    void subjectRemoval_reportsConflictingClass_andTeacherCannotUseAdminFlow() throws Exception {
        String adminToken = loginAsAdmin();
        Subject literature = subjectRepository.findByCode("VAN12").orElseThrow();
        testTeacher.setSubjects(new HashSet<>(Set.of(testSubject, literature)));
        teacherRepository.saveAndFlush(testTeacher);

        mockMvc.perform(put("/api/admin/users/teachers/{id}/subjects", testTeacher.getId())
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectIds\":[" + literature.getId() + "]}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("12A")));

        String teacherToken = loginAsTeacher();
        mockMvc.perform(get("/api/admin/users/teachers")
                .header("Authorization", authHeader(teacherToken)))
            .andExpect(status().isForbidden());
    }

    private AcademicYear createAcademicYear(String name, LocalDate startDate, LocalDate endDate) {
        AcademicYear year = new AcademicYear();
        year.setName(name);
        year.setStartDate(startDate);
        year.setEndDate(endDate);
        year.setStatus(AcademicYearStatus.DRAFT);
        return academicYearRepository.save(year);
    }

    private SchoolClass createClass(String name, AcademicYear year, int gradeLevel) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName(name);
        schoolClass.setAcademicYear(year);
        schoolClass.setGradeLevel(gradeLevel);
        schoolClass.setSchoolName("FPT Schools");
        return classRepository.save(schoolClass);
    }

    private Teacher createTeacher(String phone, String employeeCode, String name, Subject subject) {
        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("test1234"));
        user.setName(name);
        user.setRole(UserRole.TEACHER);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setEmployeeCode(employeeCode);
        teacher.setSubjects(new HashSet<>(Set.of(subject)));
        return teacherRepository.save(teacher);
    }

    private TeachingAssignment createTeachingAssignment(Teacher teacher, Subject subject, SchoolClass schoolClass,
                                                         LocalDate effectiveFrom) {
        TeachingAssignment assignment = new TeachingAssignment();
        assignment.setTeacher(teacher);
        assignment.setSubject(subject);
        assignment.setCls(schoolClass);
        assignment.setEffectiveFrom(effectiveFrom);
        assignment.setStatus(AssignmentStatus.ACTIVE);
        return teachingAssignmentRepository.save(assignment);
    }

    private HomeroomAssignment createHomeroom(Teacher teacher, SchoolClass schoolClass, AcademicYear year) {
        HomeroomAssignment assignment = new HomeroomAssignment();
        assignment.setTeacher(teacher);
        assignment.setCls(schoolClass);
        assignment.setAcademicYear(year);
        assignment.setEffectiveFrom(year.getStartDate());
        return homeroomAssignmentRepository.save(assignment);
    }

    private JsonNode getTeacherPage(String token, Long academicYearId, int size) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/users/teachers")
                .header("Authorization", authHeader(token))
                .param("academicYearId", academicYearId.toString())
                .param("page", "0")
                .param("size", String.valueOf(size)))
            .andExpect(status().isOk())
            .andReturn();
        return responseData(result);
    }

    private JsonNode getSummary(String token, Long academicYearId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/users/teachers/summary")
                .header("Authorization", authHeader(token))
                .param("academicYearId", academicYearId.toString()))
            .andExpect(status().isOk())
            .andReturn();
        return responseData(result);
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private JsonNode findTeacher(JsonNode pageData, Long teacherId) {
        for (JsonNode teacher : pageData.path("content")) {
            if (teacher.path("id").asLong() == teacherId) return teacher;
        }
        throw new AssertionError("Không tìm thấy giáo viên " + teacherId);
    }
}
