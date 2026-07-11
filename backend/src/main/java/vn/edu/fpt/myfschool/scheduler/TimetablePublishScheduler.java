package vn.edu.fpt.myfschool.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.myfschool.common.enums.TimetableStatus;
import vn.edu.fpt.myfschool.repository.TimetableRepository;
import vn.edu.fpt.myfschool.service.TimetableService;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimetablePublishScheduler {
    private final TimetableRepository timetableRepository;
    private final TimetableService timetableService;

    @Scheduled(
        cron = "${app.timetable.publish-cron:0 0 0 * * *}",
        zone = "${app.time-zone:Asia/Ho_Chi_Minh}"
    )
    public void publishDueTimetables() {
        LocalDate today = LocalDate.now();
        timetableRepository.findByStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromAsc(
            TimetableStatus.SCHEDULED, today).forEach(timetable -> {
                try {
                    timetableService.publishScheduled(timetable.getId());
                } catch (RuntimeException ex) {
                    log.error("Không thể phát hành thời khóa biểu {} theo lịch", timetable.getId(), ex);
                }
            });
    }
}
