package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.common.enums.SemesterStatus;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.Enrollment;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MobileAcademicPeriodIntegrationTest extends BaseIntegrationTest {

    @Test
    void selected_historical_period_drives_student_profile_and_schedule_class() throws Exception {
        HistoricalPeriod historical = createHistoricalPeriod();
        String token = loginAsStudent1();

        mockMvc.perform(get("/api/dashboard/student")
                .header("Authorization", authHeader(token))
                .param("academicYearId", historical.year().getId().toString())
                .param("semesterId", historical.semester().getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.studentId").value(testStudent1.getId()))
            .andExpect(jsonPath("$.data.classId").value(historical.cls().getId()))
            .andExpect(jsonPath("$.data.className").value("11A"))
            .andExpect(jsonPath("$.data.schoolName").value("FPT Schools Hòa Lạc"))
            .andExpect(jsonPath("$.data.academicYearName").value("2025-2026"))
            .andExpect(jsonPath("$.data.semesterName").value("Học kỳ 2"));

        mockMvc.perform(get("/api/schedules/me")
                .header("Authorization", authHeader(token))
                .param("semesterId", historical.semester().getId().toString())
                .param("date", "2026-03-01"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.classId").value(historical.cls().getId()))
            .andExpect(jsonPath("$.data.semesterId").value(historical.semester().getId()));
    }

    @Test
    void dashboard_rejects_semester_from_another_academic_year() throws Exception {
        HistoricalPeriod historical = createHistoricalPeriod();

        mockMvc.perform(get("/api/dashboard/student")
                .header("Authorization", authHeader(loginAsStudent1()))
                .param("academicYearId", testAcademicYear.getId().toString())
                .param("semesterId", historical.semester().getId().toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void available_periods_expose_active_current_and_completed_history_but_not_draft() throws Exception {
        HistoricalPeriod historical = createHistoricalPeriod();
        AcademicYear draft = new AcademicYear();
        draft.setName("2027-2028");
        draft.setStartDate(LocalDate.of(2027, 8, 1));
        draft.setEndDate(LocalDate.of(2028, 5, 31));
        draft.setStatus(AcademicYearStatus.DRAFT);
        draft = academicYearRepository.save(draft);
        SchoolClass draftClass = new SchoolClass();
        draftClass.setName("12B");
        draftClass.setGradeLevel(12);
        draftClass.setSchoolName("FPT Schools");
        draftClass.setAcademicYear(draft);
        draftClass = classRepository.save(draftClass);
        enroll(testStudent1.getId(), draft, draftClass);

        mockMvc.perform(get("/api/academic-years/available")
                .header("Authorization", authHeader(loginAsStudent1())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value(testAcademicYear.getId()))
            .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.data[0].semesters[0].isCurrent").value(true))
            .andExpect(jsonPath("$.data[1].id").value(historical.year().getId()))
            .andExpect(jsonPath("$.data[1].status").value("COMPLETED"));
    }

    @Test
    void parent_periods_follow_the_selected_child() throws Exception {
        createHistoricalPeriod();
        String token = loginAsParent();

        mockMvc.perform(get("/api/academic-years/available")
                .header("Authorization", authHeader(token))
                .param("studentId", testStudent1.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/academic-years/available")
                .header("Authorization", authHeader(token))
                .param("studentId", testStudent2.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value(testAcademicYear.getId()));
    }

    private HistoricalPeriod createHistoricalPeriod() {
        AcademicYear year = new AcademicYear();
        year.setName("2025-2026");
        year.setStartDate(LocalDate.of(2025, 8, 1));
        year.setEndDate(LocalDate.of(2026, 5, 31));
        year.setStatus(AcademicYearStatus.COMPLETED);
        year = academicYearRepository.save(year);

        Semester semester = new Semester();
        semester.setName("Học kỳ 2");
        semester.setAcademicYear(year);
        semester.setOrder(2);
        semester.setStartDate(LocalDate.of(2026, 1, 1));
        semester.setEndDate(LocalDate.of(2026, 5, 31));
        semester.setStatus(SemesterStatus.COMPLETED);
        semester.setIsCurrent(false);
        semester = semesterRepository.save(semester);

        SchoolClass cls = new SchoolClass();
        cls.setName("11A");
        cls.setGradeLevel(11);
        cls.setSchoolName("FPT Schools Hòa Lạc");
        cls.setAcademicYear(year);
        cls = classRepository.save(cls);
        enroll(testStudent1.getId(), year, cls);
        return new HistoricalPeriod(year, semester, cls);
    }

    private void enroll(Long studentId, AcademicYear year, SchoolClass cls) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(studentRepository.findById(studentId).orElseThrow());
        enrollment.setAcademicYear(year);
        enrollment.setCls(cls);
        enrollment.setJoinDate(year.getStartDate());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollmentRepository.save(enrollment);
    }

    private record HistoricalPeriod(AcademicYear year, Semester semester, SchoolClass cls) {}
}
