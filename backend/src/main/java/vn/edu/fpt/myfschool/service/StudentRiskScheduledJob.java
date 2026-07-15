package vn.edu.fpt.myfschool.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.SemesterStatus;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.repository.ClassRepository;
import vn.edu.fpt.myfschool.repository.SemesterRepository;

@Component
@RequiredArgsConstructor
public class StudentRiskScheduledJob {
    private final AcademicYearRepository years;
    private final SemesterRepository semesters;
    private final ClassRepository classes;
    private final StudentRiskService risks;

    @Scheduled(cron = "${app.student-risk.cron:0 15 1 * * *}")
    public void recalculateActiveScopes() {
        years.findByStatus(AcademicYearStatus.ACTIVE).forEach(year ->
                semesters.findByAcademicYearId(year.getId()).stream()
                        .filter(semester -> semester.getStatus() == SemesterStatus.ACTIVE)
                        .forEach(semester -> classes.findByAcademicYearId(year.getId())
                                .forEach(cls -> risks.recalculateClass(year.getId(), semester.getId(), cls.getId()))));
    }
}
