package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.entity.AcademicYearSubject;
import vn.edu.fpt.myfschool.repository.AcademicYearPeriodRepository;
import vn.edu.fpt.myfschool.repository.AcademicYearShiftRepository;
import vn.edu.fpt.myfschool.repository.AcademicYearSubjectRepository;
import vn.edu.fpt.myfschool.repository.HomeroomAssignmentRepository;
import vn.edu.fpt.myfschool.repository.PeriodRepository;
import vn.edu.fpt.myfschool.repository.SchoolShiftRepository;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminSetupWorkflowIntegrationTest extends BaseIntegrationTest {
    @Autowired SchoolShiftRepository shiftRepository;
    @Autowired PeriodRepository periodRepository;
    @Autowired AcademicYearSubjectRepository yearSubjectRepository;
    @Autowired AcademicYearShiftRepository yearShiftRepository;
    @Autowired AcademicYearPeriodRepository yearPeriodRepository;
    @Autowired HomeroomAssignmentRepository homeroomAssignmentRepository;

    @Test
    void assignSameTeacherToTwoHomeroomClasses_returnsConflict() throws Exception {
        String token = loginAsAdmin();
        String yearResponse = mockMvc.perform(post("/api/academic-years")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content(academicYearBody("2040-08-01", "2041-05-31")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long yearId = objectMapper.readTree(yearResponse).path("data").path("id").asLong();

        String classesResponse = mockMvc.perform(post("/api/classes/generate")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"academicYearId\":%d,\"gradeLevel\":10,\"namingPrefix\":\"H\",\"count\":2}".formatted(yearId)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long class1Id = objectMapper.readTree(classesResponse).path("data").get(0).path("id").asLong();
        long class2Id = objectMapper.readTree(classesResponse).path("data").get(1).path("id").asLong();

        mockMvc.perform(post("/api/homeroom-assignments")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classId\":%d,\"teacherId\":%d,\"academicYearId\":%d,\"effectiveFrom\":\"2040-08-01\"}"
                    .formatted(class1Id, testTeacher.getId(), yearId)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/homeroom-assignments")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classId\":%d,\"teacherId\":%d,\"academicYearId\":%d,\"effectiveFrom\":\"2040-08-01\"}"
                    .formatted(class2Id, testTeacher.getId(), yearId)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("đang là GVCN lớp")));
    }

    @Test
    void deleteDraftClass_removesHomeroomAndTeachingAssignments() throws Exception {
        String token = loginAsAdmin();
        String yearResponse = mockMvc.perform(post("/api/academic-years")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content(academicYearBody("2042-08-01", "2043-05-31")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long yearId = objectMapper.readTree(yearResponse).path("data").path("id").asLong();

        String classesResponse = mockMvc.perform(post("/api/classes/generate")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"academicYearId\":%d,\"gradeLevel\":11,\"namingPrefix\":\"D\",\"count\":1}".formatted(yearId)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long classId = objectMapper.readTree(classesResponse).path("data").get(0).path("id").asLong();

        testTeacher.getSubjects().add(testSubject);
        teacherRepository.save(testTeacher);
        AcademicYearSubject yearSubject = new AcademicYearSubject();
        yearSubject.setAcademicYear(academicYearRepository.findById(yearId).orElseThrow());
        yearSubject.setSubject(testSubject);
        yearSubjectRepository.save(yearSubject);
        mockMvc.perform(post("/api/homeroom-assignments")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classId\":%d,\"teacherId\":%d,\"academicYearId\":%d,\"effectiveFrom\":\"2042-08-01\"}"
                    .formatted(classId, testTeacher.getId(), yearId)))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/teaching-assignments")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classId\":%d,\"subjectId\":%d,\"teacherId\":%d,\"effectiveFrom\":\"2042-08-01\"}"
                    .formatted(classId, testSubject.getId(), testTeacher.getId())))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/classes/" + classId).header("Authorization", authHeader(token)))
            .andExpect(status().isOk());

        assertTrue(classRepository.findById(classId).isEmpty());
        assertTrue(homeroomAssignmentRepository.findByClsIdAndAcademicYearId(classId, yearId).isEmpty());
        assertTrue(teachingAssignmentRepository.findByClsIdAndStatus(classId,
            vn.edu.fpt.myfschool.common.enums.AssignmentStatus.ACTIVE).isEmpty());
    }

    @Test
    void manualClassCreationEndpoint_isNotAvailable() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/classes")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"10A1\",\"gradeLevel\":10,\"academicYearId\":1}"))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void completeEightStepSetup_canActivateDraftYear() throws Exception {
        String token = loginAsAdmin();
        mockMvc.perform(post("/api/master-data/initialize").header("Authorization", authHeader(token)))
            .andExpect(status().isOk());

        String yearResponse = mockMvc.perform(post("/api/academic-years")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content(academicYearBody("2032-08-01", "2033-05-31")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long yearId = objectMapper.readTree(yearResponse).path("data").path("id").asLong();
        var semesters = semesterRepository.findByAcademicYearIdOrderByOrderAsc(yearId);

        long shiftId = shiftRepository.findAllByOrderByOrderAsc().getFirst().getId();
        long periodId = periodRepository.findByShiftIdOrderByOrderAsc(shiftId).getFirst().getId();
        mockMvc.perform(put("/api/academic-years/" + yearId + "/master-data")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectIds\":[%d],\"shiftIds\":[%d],\"periodIds\":[%d]}".formatted(testSubject.getId(), shiftId, periodId)))
            .andExpect(status().isOk());

        String classResponse = mockMvc.perform(post("/api/classes/generate")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"academicYearId\":%d,\"gradeLevel\":10,\"namingPrefix\":\"A\",\"count\":1}".formatted(yearId)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long classId = objectMapper.readTree(classResponse).path("data").get(0).path("id").asLong();

        testTeacher.getSubjects().add(testSubject);
        teacherRepository.save(testTeacher);
        mockMvc.perform(post("/api/homeroom-assignments")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classId\":%d,\"teacherId\":%d,\"academicYearId\":%d,\"effectiveFrom\":\"2032-08-01\"}".formatted(classId, testTeacher.getId(), yearId)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/student-enrollments")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"academicYearId":%d,"classId":%d,"studentCode":"HS203201","studentName":"Học sinh workflow",
                    "dateOfBirth":"2016-01-10","gender":"FEMALE","parentName":"PH Test","relationship":"MOTHER","parentPhone":"0909000002"}
                    """.formatted(yearId, classId)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/teaching-assignments")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classId\":%d,\"subjectId\":%d,\"teacherId\":%d}"
                    .formatted(classId, testSubject.getId(), testTeacher.getId())))
            .andExpect(status().isOk());

        assertEquals(1, teachingAssignmentRepository
            .findByClsIdAndStatus(classId, vn.edu.fpt.myfschool.common.enums.AssignmentStatus.ACTIVE).size());
        mockMvc.perform(get("/api/teaching-assignments")
                .param("classId", String.valueOf(classId))
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/academic-years/" + yearId + "/readiness").header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.ready").value(true))
            .andExpect(jsonPath("$.data.checks[?(@.code == 'TIMETABLES')]").isEmpty());

        testAcademicYear.setStatus(AcademicYearStatus.COMPLETED);
        academicYearRepository.save(testAcademicYear);
        mockMvc.perform(post("/api/academic-years/" + yearId + "/activate").header("Authorization", authHeader(token)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/academic-years").header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.id == %d)].status".formatted(yearId)).value("ACTIVE"));
    }

    @Test
    void updateMasterData_addKeepAndRemoveSelections_isIdempotent() throws Exception {
        String token = loginAsAdmin();
        mockMvc.perform(post("/api/master-data/initialize").header("Authorization", authHeader(token)))
            .andExpect(status().isOk());

        String yearResponse = mockMvc.perform(post("/api/academic-years")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content(academicYearBody("2034-08-01", "2035-05-31")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long yearId = objectMapper.readTree(yearResponse).path("data").path("id").asLong();

        long subject1 = testSubject.getId();
        long subject2 = subjectRepository.findByCode("VAN12").orElseThrow().getId();
        var shifts = shiftRepository.findAllByOrderByOrderAsc();
        long shift1 = shifts.get(0).getId();
        long shift2 = shifts.get(1).getId();
        long period1 = periodRepository.findByShiftIdOrderByOrderAsc(shift1).getFirst().getId();
        long period2 = periodRepository.findByShiftIdOrderByOrderAsc(shift2).getFirst().getId();

        saveMasterData(token, yearId,
            "{\"subjectIds\":[%d],\"shiftIds\":[%d],\"periodIds\":[%d]}".formatted(subject1, shift1, period1));

        String expanded = "{\"subjectIds\":[%d,%d],\"shiftIds\":[%d,%d],\"periodIds\":[%d,%d]}"
            .formatted(subject1, subject2, shift1, shift2, period1, period2);
        saveMasterData(token, yearId, expanded);
        saveMasterData(token, yearId, expanded);

        assertEquals(Set.of(subject1, subject2), yearSubjectRepository.findByAcademicYearId(yearId).stream()
            .map(item -> item.getSubject().getId()).collect(Collectors.toSet()));
        assertEquals(2, yearShiftRepository.findByAcademicYearId(yearId).size());
        assertEquals(2, yearPeriodRepository.findByAcademicYearId(yearId).size());

        saveMasterData(token, yearId,
            "{\"subjectIds\":[%d],\"shiftIds\":[%d],\"periodIds\":[%d]}".formatted(subject2, shift2, period2));

        assertEquals(Set.of(subject2), yearSubjectRepository.findByAcademicYearId(yearId).stream()
            .map(item -> item.getSubject().getId()).collect(Collectors.toSet()));
        assertEquals(Set.of(shift2), yearShiftRepository.findByAcademicYearId(yearId).stream()
            .map(item -> item.getShift().getId()).collect(Collectors.toSet()));
        assertEquals(Set.of(period2), yearPeriodRepository.findByAcademicYearId(yearId).stream()
            .map(item -> item.getPeriod().getId()).collect(Collectors.toSet()));
    }

    private void saveMasterData(String token, long yearId, String body) throws Exception {
        mockMvc.perform(put("/api/academic-years/" + yearId + "/master-data")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());
    }
}
