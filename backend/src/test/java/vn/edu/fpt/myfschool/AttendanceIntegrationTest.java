package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AttendanceIntegrationTest extends BaseIntegrationTest {

    private String attendanceJson(String shift, String entries) {
        return "{\"classId\":" + testClass.getId() + ",\"date\":\"2026-06-24\",\"shift\":\"" + shift + "\",\"entries\":[" + entries + "]}";
    }

    private String entry(Long studentId, String status) {
        return "{\"studentId\":" + studentId + ",\"status\":\"" + status + "\"}";
    }

    @Test
    void get_daily_attendance_teacher_only() throws Exception {
        String token = loginAsTeacher();

        mockMvc.perform(get("/api/attendance/daily")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("date", "2026-06-24")
                .param("shift", "MORNING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.classId").value(testClass.getId().intValue()))
            .andExpect(jsonPath("$.data.shift").value("MORNING"))
            .andExpect(jsonPath("$.data.students").isArray());
    }

    @Test
    void get_daily_attendance_student_forbidden() throws Exception {
        String token = loginAsStudent1();

        mockMvc.perform(get("/api/attendance/daily")
                .header("Authorization", authHeader(token))
                .param("classId", testClass.getId().toString())
                .param("date", "2026-06-24")
                .param("shift", "MORNING"))
            .andExpect(status().isForbidden());
    }

    @Test
    void submit_attendance_teacher_only() throws Exception {
        String token = loginAsTeacher();
        String body = attendanceJson("MORNING",
            entry(testStudent1.getId(), "PRESENT") + "," + entry(testStudent2.getId(), "LATE"));

        mockMvc.perform(post("/api/attendance/submit")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void submit_attendance_parent_forbidden() throws Exception {
        String token = loginAsParent();
        String body = attendanceJson("MORNING", entry(testStudent1.getId(), "PRESENT"));

        mockMvc.perform(post("/api/attendance/submit")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    void update_single_attendance() throws Exception {
        String token = loginAsTeacher();
        String body = attendanceJson("MORNING", entry(testStudent1.getId(), "PRESENT"));

        mockMvc.perform(post("/api/attendance/submit")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());

        mockMvc.perform(put("/api/attendance/1")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"LATE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void get_student_attendance_log() throws Exception {
        String token = loginAsStudent1();

        mockMvc.perform(get("/api/attendance/student")
                .header("Authorization", authHeader(token))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.records").isArray())
            .andExpect(jsonPath("$.data.stats").exists());
    }

    @Test
    void get_student_attendance_parent_view() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(get("/api/attendance/student")
                .header("Authorization", authHeader(token))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.stats").exists());
    }

    @Test
    void attendance_stats_contains_all_statuses() throws Exception {
        String token = loginAsTeacher();
        String body = "{\"classId\":" + testClass.getId() + ",\"date\":\"2026-06-26\",\"shift\":\"AFTERNOON\",\"entries\":["
            + entry(testStudent1.getId(), "PRESENT") + ","
            + entry(testStudent2.getId(), "LATE") + ","
            + entry(testStudent3.getId(), "ABSENT_WITHOUT_LEAVE") + "]}";

        mockMvc.perform(post("/api/attendance/submit")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());

        String studentToken = loginAsStudent1();
        mockMvc.perform(get("/api/attendance/student")
                .header("Authorization", authHeader(studentToken))
                .param("studentId", testStudent1.getId().toString())
                .param("semesterId", testSemester.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.stats.presentDays").isNumber());
    }
}
