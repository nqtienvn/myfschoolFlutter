package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.enums.TimetableStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.AcademicYearPeriod;
import vn.edu.fpt.myfschool.entity.AcademicYearShift;
import vn.edu.fpt.myfschool.entity.Schedule;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.Subject;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.TeachingAssignment;
import vn.edu.fpt.myfschool.entity.Timetable;
import vn.edu.fpt.myfschool.entity.User;

import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TimetableAutoGenerationIntegrationTest extends BaseIntegrationTest {
    @Autowired vn.edu.fpt.myfschool.repository.ScheduleRepository scheduleRepository;
    @Autowired vn.edu.fpt.myfschool.repository.TimetableRepository timetableRepository;

    @Test
    void auto_generate_selected_classes_avoids_teacher_conflicts_and_isolated_other_year() throws Exception {
        SchoolClass secondClass = classRepository.findByNameAndAcademicYearId("SE1913", testAcademicYear.getId())
            .orElseThrow();
        Subject literature = subjectRepository.findByCode("VAN12").orElseThrow();
        Teacher secondTeacher = createTeacher("0909111101", "GV201", "GV Văn");
        createAssignment(secondClass, testSubject, testTeacher, testSemester.getStartDate());
        createAssignment(secondClass, literature, secondTeacher, testSemester.getStartDate());

        Timetable otherYearTimetable = createOtherYearDraftWithSlot();
        String body = """
            {"academicYearId":%d,"semesterId":%d,"classIds":[%d,%d],"shiftIds":[%d],"periodIds":[%d]}
            """.formatted(testAcademicYear.getId(), testSemester.getId(), testClass.getId(), secondClass.getId(),
                testMorningShift.getId(), testPeriods.getFirst().getId());

        var response = mockMvc.perform(post("/api/timetables/auto-generate")
                .header("Authorization", authHeader(loginAsAdmin()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.classCount").value(2))
            .andExpect(jsonPath("$.data.timetableCount").value(2))
            .andExpect(jsonPath("$.data.slotCount").value(12))
            .andReturn();

        var generated = objectMapper.readTree(response.getResponse().getContentAsString()).path("data").path("timetables");
        assertThat(generated).hasSize(2);
        assertThat(generated.findValues("classId")).extracting(node -> node.asLong())
            .containsExactlyInAnyOrder(testClass.getId(), secondClass.getId());

        var generatedSchedules = scheduleRepository.findPlanningSchedules(
                testSemester.getId(), java.util.Set.of(TimetableStatus.DRAFT)).stream()
            .filter(slot -> java.util.Set.of(testClass.getId(), secondClass.getId())
                .contains(slot.getTimetable().getCls().getId()))
            .toList();
        assertThat(generatedSchedules).hasSize(12);
        for (int day = 2; day <= 7; day++) {
            int targetDay = day;
            var teacherIds = generatedSchedules.stream()
                .filter(slot -> slot.getDayOfWeek() == targetDay)
                .map(slot -> slot.getAssignment().getTeacher().getId())
                .collect(java.util.stream.Collectors.toSet());
            assertThat(teacherIds).hasSize(2);
        }
        assertThat(scheduleRepository.findByTimetableIdOrderByDayOfWeekAscPeriodAsc(otherYearTimetable.getId()))
            .hasSize(1);
    }

    @Test
    void empty_class_selection_generates_all_classes_only_in_selected_year() throws Exception {
        SchoolClass secondClass = classRepository.findByNameAndAcademicYearId("SE1913", testAcademicYear.getId())
            .orElseThrow();
        Subject literature = subjectRepository.findByCode("VAN12").orElseThrow();
        Teacher secondTeacher = createTeacher("0909111103", "GV203", "GV Toàn năm");
        createAssignment(secondClass, literature, secondTeacher, testSemester.getStartDate());
        Timetable otherYearTimetable = createOtherYearDraftWithSlot();

        String body = """
            {"academicYearId":%d,"semesterId":%d,"classIds":[],"shiftIds":[%d],"periodIds":[%d]}
            """.formatted(testAcademicYear.getId(), testSemester.getId(),
                testMorningShift.getId(), testPeriods.getFirst().getId());
        var response = mockMvc.perform(post("/api/timetables/auto-generate")
                .header("Authorization", authHeader(loginAsAdmin()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.classCount").value(2))
            .andExpect(jsonPath("$.data.slotCount").value(12))
            .andReturn();

        var timetables = objectMapper.readTree(response.getResponse().getContentAsString())
            .path("data").path("timetables");
        assertThat(timetables.findValues("classId")).extracting(node -> node.asLong())
            .containsExactlyInAnyOrder(testClass.getId(), secondClass.getId());
        assertThat(scheduleRepository.findByTimetableIdOrderByDayOfWeekAscPeriodAsc(otherYearTimetable.getId()))
            .hasSize(1);
    }

    @Test
    void availability_hides_busy_teacher_and_server_rejects_manual_conflict() throws Exception {
        SchoolClass secondClass = classRepository.findByNameAndAcademicYearId("SE1913", testAcademicYear.getId())
            .orElseThrow();
        Subject literature = subjectRepository.findByCode("VAN12").orElseThrow();
        Teacher secondTeacher = createTeacher("0909111102", "GV202", "GV Văn 2");
        TeachingAssignment busyAssignment = createAssignment(
            secondClass, testSubject, testTeacher, testSemester.getStartDate());
        TeachingAssignment freeAssignment = createAssignment(
            secondClass, literature, secondTeacher, testSemester.getStartDate());

        long firstDraftId = createDraft(testClass);
        createSlot(firstDraftId, testTeachingAssignment.getId());

        var availabilityResponse = mockMvc.perform(get("/api/schedules/assignment-availability")
                .header("Authorization", authHeader(loginAsAdmin()))
                .param("classId", secondClass.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andReturn();
        var availability = objectMapper.readTree(availabilityResponse.getResponse().getContentAsString()).path("data");
        var targetSlot = new java.util.ArrayList<Long>();
        availability.forEach(item -> {
            if (item.path("dayOfWeek").asInt() == 2
                    && item.path("periodId").asLong() == testPeriods.getFirst().getId()) {
                item.path("assignmentIds").forEach(id -> targetSlot.add(id.asLong()));
            }
        });
        assertThat(targetSlot).contains(freeAssignment.getId()).doesNotContain(busyAssignment.getId());

        long secondDraftId = createDraft(secondClass);
        String conflictBody = """
            {"timetableId":%d,"assignmentId":%d,"dayOfWeek":2,"periodId":%d}
            """.formatted(secondDraftId, busyAssignment.getId(), testPeriods.getFirst().getId());
        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(loginAsAdmin()))
                .contentType(MediaType.APPLICATION_JSON).content(conflictBody))
            .andExpect(status().isConflict());
    }

    private long createDraft(SchoolClass cls) throws Exception {
        String body = """
            {"classId":%d,"semesterId":%d,"effectiveFrom":"2026-09-01"}
            """.formatted(cls.getId(), testSemester.getId());
        var result = mockMvc.perform(post("/api/timetables")
                .header("Authorization", authHeader(loginAsAdmin()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private void createSlot(long timetableId, long assignmentId) throws Exception {
        String body = """
            {"timetableId":%d,"assignmentId":%d,"dayOfWeek":2,"periodId":%d}
            """.formatted(timetableId, assignmentId, testPeriods.getFirst().getId());
        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(loginAsAdmin()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());
    }

    private Teacher createTeacher(String phone, String code, String name) {
        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("test1234"));
        user.setName(name);
        user.setRole(UserRole.TEACHER);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setEmployeeCode(code);
        return teacherRepository.save(teacher);
    }

    private TeachingAssignment createAssignment(SchoolClass cls, Subject subject, Teacher teacher, LocalDate from) {
        TeachingAssignment assignment = new TeachingAssignment();
        assignment.setCls(cls);
        assignment.setSubject(subject);
        assignment.setTeacher(teacher);
        assignment.setEffectiveFrom(from);
        assignment.setStatus(AssignmentStatus.ACTIVE);
        return teachingAssignmentRepository.save(assignment);
    }

    private Timetable createOtherYearDraftWithSlot() {
        AcademicYear otherYear = new AcademicYear();
        otherYear.setName("2027-2028-auto");
        otherYear.setStartDate(LocalDate.of(2027, 8, 1));
        otherYear.setEndDate(LocalDate.of(2028, 5, 31));
        otherYear.setStatus(AcademicYearStatus.DRAFT);
        otherYear = academicYearRepository.save(otherYear);

        AcademicYearShift appliedShift = new AcademicYearShift();
        appliedShift.setAcademicYear(otherYear);
        appliedShift.setShift(testMorningShift);
        academicYearShiftRepository.save(appliedShift);
        AcademicYearPeriod appliedPeriod = new AcademicYearPeriod();
        appliedPeriod.setAcademicYear(otherYear);
        appliedPeriod.setPeriod(testPeriods.getFirst());
        academicYearPeriodRepository.save(appliedPeriod);

        Semester semester = new Semester();
        semester.setName("HK I");
        semester.setAcademicYear(otherYear);
        semester.setOrder(1);
        semester.setStartDate(LocalDate.of(2027, 9, 1));
        semester.setEndDate(LocalDate.of(2028, 1, 15));
        semester.setIsCurrent(false);
        semester = semesterRepository.save(semester);

        SchoolClass cls = new SchoolClass();
        cls.setName("12A-OTHER");
        cls.setGradeLevel(12);
        cls.setAcademicYear(otherYear);
        cls.setSchoolName("FPT Schools");
        cls = classRepository.save(cls);
        TeachingAssignment assignment = createAssignment(cls, testSubject, testTeacher, semester.getStartDate());

        Timetable timetable = new Timetable();
        timetable.setCls(cls);
        timetable.setSemester(semester);
        timetable.setVersion(1);
        timetable.setStatus(TimetableStatus.DRAFT);
        timetable.setEffectiveFrom(semester.getStartDate());
        timetable.setEffectiveTo(semester.getEndDate());
        timetable = timetableRepository.save(timetable);

        Schedule slot = new Schedule();
        slot.setTimetable(timetable);
        slot.setAssignment(assignment);
        slot.setDayOfWeek(2);
        slot.setPeriod(1);
        slot.setPeriodRef(testPeriods.getFirst());
        slot.setRoom(cls.getName());
        slot.setShift(Shift.MORNING);
        scheduleRepository.save(slot);
        return timetable;
    }
}
