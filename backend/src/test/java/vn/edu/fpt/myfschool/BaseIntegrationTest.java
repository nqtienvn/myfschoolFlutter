package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.enums.*;
import vn.edu.fpt.myfschool.controller.entity.ClassSubject;
import vn.edu.fpt.myfschool.controller.entity.Parent;
import vn.edu.fpt.myfschool.controller.entity.SchoolClass;
import vn.edu.fpt.myfschool.controller.entity.Semester;
import vn.edu.fpt.myfschool.controller.entity.Student;
import vn.edu.fpt.myfschool.controller.entity.StudentGuardian;
import vn.edu.fpt.myfschool.controller.entity.Subject;
import vn.edu.fpt.myfschool.controller.entity.Teacher;
import vn.edu.fpt.myfschool.controller.entity.User;
import vn.edu.fpt.myfschool.repository.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected ClassRepository classRepository;
    @Autowired protected SubjectRepository subjectRepository;
    @Autowired protected SemesterRepository semesterRepository;
    @Autowired protected UserRepository userRepository;
    @Autowired protected TeacherRepository teacherRepository;
    @Autowired protected StudentRepository studentRepository;
    @Autowired protected ParentRepository parentRepository;
    @Autowired protected StudentGuardianRepository studentGuardianRepository;
    @Autowired protected ClassSubjectRepository classSubjectRepository;
    @Autowired protected ConversationRepository conversationRepository;
    @Autowired protected ConversationParticipantRepository conversationParticipantRepository;
    @Autowired protected MessageRepository messageRepository;
    @Autowired protected vn.edu.fpt.myfschool.service.ConversationService conversationService;
    @Autowired protected vn.edu.fpt.myfschool.service.MessageService messageService;
    @Autowired protected org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    protected SchoolClass testClass;
    protected Subject testSubject;
    protected Semester testSemester;
    protected Teacher testTeacher;
    protected Student testStudent1;
    protected Student testStudent2;
    protected Student testStudent3;
    protected Parent testParent;
    protected User testAdminUser;

    @BeforeEach
    void setUpTestData() {
        // Class
        testClass = new SchoolClass();
        testClass.setName("12A");
        testClass.setGradeLevel(12);
        testClass.setAcademicYear("2026-2027");
        testClass.setSchoolName("FPT Schools");
        testClass = classRepository.save(testClass);

        SchoolClass class3 = new SchoolClass();
        class3.setName("SE1913");
        class3.setGradeLevel(12);
        class3.setAcademicYear("2026-2027");
        class3.setSchoolName("FPT Schools");
        classRepository.save(class3);

        // Subject
        testSubject = new Subject();
        testSubject.setName("Toán");
        testSubject.setCode("TOAN12");
        testSubject = subjectRepository.save(testSubject);

        Subject subject2 = new Subject();
        subject2.setName("Văn");
        subject2.setCode("VAN12");
        subjectRepository.save(subject2);

        // Semester
        testSemester = new Semester();
        testSemester.setName("HK I");
        testSemester.setAcademicYear("2026-2027");
        testSemester.setStartDate(LocalDate.of(2026, 9, 1));
        testSemester.setEndDate(LocalDate.of(2027, 1, 15));
        testSemester.setIsCurrent(true);
        testSemester = semesterRepository.save(testSemester);

        // Teacher user + teacher
        User teacherUser = new User();
        teacherUser.setPhone("0909000001");
        teacherUser.setPassword(passwordEncoder.encode("test1234"));
        teacherUser.setName("GV Test");
        teacherUser.setRole(UserRole.TEACHER);
        teacherUser.setStatus(UserStatus.ACTIVE);
        teacherUser = userRepository.save(teacherUser);

        testTeacher = new Teacher();
        testTeacher.setUser(teacherUser);
        testTeacher.setEmployeeCode("GV100");
        testTeacher.setDepartment("PRM393");
        testTeacher = teacherRepository.save(testTeacher);

        // ClassSubject
        ClassSubject cs = new ClassSubject();
        cs.setCls(testClass);
        cs.setSubject(testSubject);
        cs.setTeacher(testTeacher);
        cs.setIsHomeroom(false);
        cs.setAcademicYear("2026-2027");
        classSubjectRepository.save(cs);

        // Parent user + parent
        User parentUser = new User();
        parentUser.setPhone("0909000002");
        parentUser.setPassword(passwordEncoder.encode("test1234"));
        parentUser.setName("PH Test");
        parentUser.setRole(UserRole.PARENT);
        parentUser.setStatus(UserStatus.ACTIVE);
        parentUser = userRepository.save(parentUser);

        testParent = new Parent();
        testParent.setUser(parentUser);
        testParent.setAddress("Hà Nội");
        testParent = parentRepository.save(testParent);

        // Admin user
        User adminUser = new User();
        adminUser.setPhone("0909000009");
        adminUser.setPassword(passwordEncoder.encode("test1234"));
        adminUser.setName("Admin Test");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);
        testAdminUser = userRepository.save(adminUser);

        // Student users + students
        for (int i = 1; i <= 3; i++) {
            User sUser = new User();
            sUser.setPhone("09090000" + (10 + i));
            sUser.setPassword(passwordEncoder.encode("test1234"));
            sUser.setName("HS " + i);
            sUser.setRole(UserRole.STUDENT);
            sUser.setStatus(UserStatus.ACTIVE);
            sUser = userRepository.save(sUser);

            Student s = new Student();
            s.setUser(sUser);
            s.setStudentCode("12A-0" + i);
            s.setCurrentClass(testClass);
            s.setDateOfBirth(LocalDate.of(2008, 1, i * 5));
            s = studentRepository.save(s);

            if (i == 1) testStudent1 = s;
            else if (i == 2) testStudent2 = s;
            else testStudent3 = s;

            // Link to parent
            StudentGuardian sg = new StudentGuardian();
            sg.setStudent(s);
            sg.setGuardian(testParent);
            sg.setRelationship(Relationship.FATHER);
            studentGuardianRepository.save(sg);
        }
    }

    protected String registerUser(String phone, String password, String name, String role,
                                   String studentCode, String employeeCode) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"phone\":\"").append(phone).append("\"");
        sb.append(",\"password\":\"").append(password).append("\"");
        sb.append(",\"name\":\"").append(name).append("\"");
        sb.append(",\"role\":\"").append(role).append("\"");
        if (studentCode != null) sb.append(",\"studentCode\":\"").append(studentCode).append("\"");
        if (employeeCode != null) sb.append(",\"employeeCode\":\"").append(employeeCode).append("\"");
        sb.append("}");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sb.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("data").get("token").asText();
    }

    protected String login(String phone, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + phone + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("data").get("token").asText();
    }

    protected String authHeader(String token) {
        return "Bearer " + token;
    }

    protected String loginAsTeacher() throws Exception {
        return login("0909000001", "test1234");
    }

    protected String loginAsParent() throws Exception {
        return login("0909000002", "test1234");
    }

    protected String loginAsStudent1() throws Exception {
        return login("0909000011", "test1234");
    }

    protected String loginAsStudent2() throws Exception {
        return login("0909000012", "test1234");
    }

    protected String loginAsAdmin() throws Exception {
        return login("0909000009", "test1234");
    }
}
