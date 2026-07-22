package vn.edu.fpt.myfschool;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.common.enums.GradeEntryRole;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.entity.AcademicYearSubject;
import vn.edu.fpt.myfschool.entity.Enrollment;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.AcademicYearGradeConfigItemRepository;
import vn.edu.fpt.myfschool.repository.SemesterResultRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SchoolSemesterResultIntegrationTest extends BaseIntegrationTest {

    @Autowired AcademicYearGradeConfigItemRepository configItems;
    @Autowired SemesterResultRepository semesterResults;

    @BeforeEach
    void prepareSchoolWideGradeEntry() {
        AcademicYearSubject applied = new AcademicYearSubject();
        applied.setAcademicYear(testAcademicYear);
        applied.setSubject(testSubject);
        academicYearSubjectRepository.save(applied);
        var configuredItems = configItems.findByConfigAcademicYearIdOrderByDisplayOrderAsc(testAcademicYear.getId());
        configuredItems.forEach(item -> item.setEntryRole(GradeEntryRole.SUBJECT_TEACHER_AND_ADMIN));
        configItems.saveAllAndFlush(configuredItems);
    }

    @Test
    void calculatesAndPublishesResultsForEveryActiveClassInTheSchool() throws Exception {
        SchoolClass secondClass = new SchoolClass();
        secondClass.setName("12B");
        secondClass.setGradeLevel(12);
        secondClass.setAcademicYear(testAcademicYear);
        secondClass.setSchoolName("FPT Schools");
        secondClass = classRepository.save(secondClass);
        Student secondClassStudent = enroll(secondClass, "12B-01", "0909000098");

        String adminToken = loginAsAdmin();
        JsonNode firstBook = gradeBook(adminToken, testClass.getId());
        JsonNode secondBook = gradeBook(adminToken, secondClass.getId());
        submitAllItems(adminToken, firstBook, List.of(testStudent1, testStudent2, testStudent3));
        submitAllItems(adminToken, secondBook, List.of(secondClassStudent));

        mockMvc.perform(post("/api/semester-results/calculate-school")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"academicYearId\":" + testAcademicYear.getId()
                        + ",\"semesterId\":" + testSemester.getId() + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.updated").value(4));

        mockMvc.perform(post("/api/semester-results/admin/publish-school")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"academicYearId\":" + testAcademicYear.getId()
                        + ",\"semesterId\":" + testSemester.getId() + "}"))
            .andExpect(status().isOk());

        var results = semesterResults.findBySemesterId(testSemester.getId());
        assertEquals(4, results.size());
        assertTrue(results.stream().allMatch(result -> result.getPublishedAt() != null));
    }

    private Student enroll(SchoolClass cls, String code, String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("test1234"));
        user.setName("HS " + code);
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setStudentCode(code);
        student.setCurrentClass(cls);
        student.setDateOfBirth(LocalDate.of(2008, 2, 1));
        student = studentRepository.save(student);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCls(cls);
        enrollment.setAcademicYear(testAcademicYear);
        enrollment.setJoinDate(testAcademicYear.getStartDate());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollmentRepository.save(enrollment);
        return student;
    }

    private JsonNode gradeBook(String token, Long classId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/grade-books")
                .header("Authorization", authHeader(token))
                .param("classId", classId.toString())
                .param("subjectId", testSubject.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private void submitAllItems(String token, JsonNode gradeBook, List<Student> students) throws Exception {
        int score = 7;
        for (JsonNode item : gradeBook.get("items")) {
            StringBuilder entries = new StringBuilder();
            for (Student student : students) {
                if (!entries.isEmpty()) entries.append(',');
                entries.append("{\"studentId\":").append(student.getId())
                        .append(",\"score\":").append(score).append('}');
            }
            mockMvc.perform(put("/api/grade-books/scores")
                    .header("Authorization", authHeader(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"gradeItemId\":" + item.get("id").asLong()
                            + ",\"entries\":[" + entries + "]}"))
                .andExpect(status().isOk());
            score++;
        }
    }
}
