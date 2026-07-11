package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.repository.NotificationRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ScheduleIntegrationTest extends BaseIntegrationTest {

    @Autowired NotificationRepository notificationRepository;

    private String schedJson(long timetableId, int day, int period, String shift) {
        return "{\"timetableId\":" + timetableId + ",\"assignmentId\":" + testTeachingAssignment.getId()
            + ",\"dayOfWeek\":" + day + ",\"period\":" + period
            + ",\"shift\":\"" + shift + "\"}";
    }

    private long createDraft(String token, String effectiveFrom, Long copyFrom) throws Exception {
        String body = "{\"classId\":%d,\"semesterId\":%d,\"effectiveFrom\":\"%s\"%s}"
            .formatted(testClass.getId(), testSemester.getId(), effectiveFrom,
                copyFrom == null ? "" : ",\"copyFromTimetableId\":" + copyFrom);
        var result = mockMvc.perform(post("/api/timetables")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private void publish(String token, long timetableId, String effectiveFrom) throws Exception {
        mockMvc.perform(post("/api/timetables/" + timetableId + "/publish")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"effectiveFrom\":\"" + effectiveFrom + "\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void parent_gets_linked_student_schedule() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(get("/api/schedules/student/" + testStudent1.getId())
                .header("Authorization", authHeader(token))
                .param("date", "2026-09-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.days").isArray())
            .andExpect(jsonPath("$.data.days.length()").value(7));
    }

    @Test
    void current_schedule_uses_current_semester_from_active_year_when_other_year_is_also_current() throws Exception {
        var otherYear = new vn.edu.fpt.myfschool.entity.AcademicYear();
        otherYear.setName("2027-2028");
        otherYear.setStartDate(java.time.LocalDate.of(2027, 8, 1));
        otherYear.setEndDate(java.time.LocalDate.of(2028, 5, 31));
        otherYear.setStatus(vn.edu.fpt.myfschool.common.enums.AcademicYearStatus.DRAFT);
        otherYear = academicYearRepository.save(otherYear);

        var otherSemester = new vn.edu.fpt.myfschool.entity.Semester();
        otherSemester.setName("HK I");
        otherSemester.setAcademicYear(otherYear);
        otherSemester.setOrder(1);
        otherSemester.setStartDate(java.time.LocalDate.of(2027, 9, 1));
        otherSemester.setEndDate(java.time.LocalDate.of(2028, 1, 15));
        otherSemester.setIsCurrent(true);
        semesterRepository.save(otherSemester);

        mockMvc.perform(get("/api/schedules/me")
                .header("Authorization", authHeader(loginAsStudent1())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.semesterId").value(testSemester.getId()));
    }

    @Test
    void parent_cannot_get_unlinked_student_schedule() throws Exception {
        var link = studentGuardianRepository
            .findByStudentIdAndGuardianId(testStudent1.getId(), testParent.getId())
            .orElseThrow();
        studentGuardianRepository.delete(link);
        studentGuardianRepository.flush();

        mockMvc.perform(get("/api/schedules/student/" + testStudent1.getId())
                .header("Authorization", authHeader(loginAsParent())))
            .andExpect(status().isForbidden());
    }

    @Test
    void student_and_teacher_get_their_own_schedules() throws Exception {
        String adminToken = loginAsAdmin();
        long timetableId = createDraft(adminToken, "2026-09-01", null);
        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(timetableId, 2, 1, "MORNING")))
            .andExpect(status().isOk());
        publish(adminToken, timetableId, "2026-09-01");

        org.assertj.core.api.Assertions.assertThat(
            notificationRepository.findByUserIdOrderByCreatedAtDesc(testStudent1.getUser().getId()))
            .anyMatch(item -> "Thời khóa biểu".equals(item.getTag()));
        org.assertj.core.api.Assertions.assertThat(
            notificationRepository.findByUserIdOrderByCreatedAtDesc(testParent.getUser().getId()))
            .anyMatch(item -> "Thời khóa biểu".equals(item.getTag()));
        org.assertj.core.api.Assertions.assertThat(
            notificationRepository.findByUserIdOrderByCreatedAtDesc(testTeacher.getUser().getId()))
            .anyMatch(item -> "Thời khóa biểu".equals(item.getTag()));

        mockMvc.perform(get("/api/schedules/me")
                .header("Authorization", authHeader(loginAsStudent1()))
                .param("date", "2026-09-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.classId").value(testClass.getId()))
            .andExpect(jsonPath("$.data.days[1].morningSlots[0].subjectName").value(testSubject.getName()));

        mockMvc.perform(get("/api/schedules/me")
                .header("Authorization", authHeader(loginAsTeacher()))
                .param("date", "2026-09-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.days[1].morningSlots[0].className").value(testClass.getName()))
            .andExpect(jsonPath("$.data.days[1].morningSlots[0].teacherId").value(testTeacher.getId()));
    }

    @Test
    void admin_can_schedule_publication_without_exposing_timetable_early() throws Exception {
        String token = loginAsAdmin();
        long timetableId = createDraft(token, "2026-09-01", null);
        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(timetableId, 2, 1, "MORNING")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/timetables/" + timetableId + "/schedule")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"effectiveFrom\":\"2026-09-15\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SCHEDULED"));

        mockMvc.perform(get("/api/schedules/class")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString())
                .param("date", "2026-09-20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.timetableId").doesNotExist());
    }

    @Test
    void create_schedule_admin_only() throws Exception {
        String token = loginAsAdmin();
        long timetableId = createDraft(token, "2026-09-01", null);

        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(timetableId, 2, 1, "MORNING")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.dayOfWeek").value(2))
            .andExpect(jsonPath("$.data.period").value(1))
            .andExpect(jsonPath("$.data.room").value(testClass.getName()));
    }

    @Test
    void create_schedule_duplicate_class_period_fails() throws Exception {
        String token = loginAsAdmin();
        long timetableId = createDraft(token, "2026-09-01", null);

        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(timetableId, 3, 2, "MORNING")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(timetableId, 3, 2, "MORNING")))
            .andExpect(status().isConflict());
    }

    @Test
    void delete_schedule() throws Exception {
        String token = loginAsAdmin();
        long timetableId = createDraft(token, "2026-09-01", null);

        var createResult = mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(timetableId, 5, 4, "AFTERNOON")))
            .andExpect(status().isOk())
            .andReturn();

        Long scheduleId = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(createResult.getResponse().getContentAsString())
            .get("data").get("id").asLong();

        mockMvc.perform(delete("/api/schedules/" + scheduleId).header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void get_available_periods() throws Exception {
        String token = loginAsAdmin();
        long timetableId = createDraft(token, "2026-09-01", null);

        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(timetableId, 6, 1, "MORNING")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/schedules/available-periods")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString())
                .param("dayOfWeek", "6")
                .param("shift", "MORNING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(4));
    }

    @Test
    void student_forbidden_to_create_schedule() throws Exception {
        String adminToken = loginAsAdmin();
        long timetableId = createDraft(adminToken, "2026-09-01", null);
        String token = loginAsStudent1();

        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(timetableId, 2, 1, "MORNING")))
            .andExpect(status().isForbidden());
    }

    @Test
    void publish_new_version_keeps_old_schedule_by_effective_date() throws Exception {
        String token = loginAsAdmin();
        long version1 = createDraft(token, "2026-09-01", null);
        mockMvc.perform(post("/api/schedules")
                .header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content(schedJson(version1, 2, 1, "MORNING")))
            .andExpect(status().isOk());
        publish(token, version1, "2026-09-01");

        long version2 = createDraft(token, "2026-11-01", version1);
        publish(token, version2, "2026-11-01");

        mockMvc.perform(get("/api/schedules/class")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString())
                .param("date", "2026-10-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.timetableVersion").value(1));

        mockMvc.perform(get("/api/schedules/class")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("semesterId", testSemester.getId().toString())
                .param("date", "2026-11-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.timetableVersion").value(2));
    }
}
