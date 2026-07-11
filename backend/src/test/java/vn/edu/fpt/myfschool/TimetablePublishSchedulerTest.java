package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.myfschool.common.enums.TimetableStatus;
import vn.edu.fpt.myfschool.entity.Timetable;
import vn.edu.fpt.myfschool.repository.TimetableRepository;
import vn.edu.fpt.myfschool.scheduler.TimetablePublishScheduler;
import vn.edu.fpt.myfschool.service.TimetableService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetablePublishSchedulerTest {
    @Mock TimetableRepository timetableRepository;
    @Mock TimetableService timetableService;
    @InjectMocks TimetablePublishScheduler scheduler;

    @Test
    void publishes_every_scheduled_timetable_due_today() {
        Timetable first = new Timetable();
        first.setId(10L);
        Timetable second = new Timetable();
        second.setId(11L);
        when(timetableRepository.findByStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromAsc(
            any(TimetableStatus.class), any(LocalDate.class))).thenReturn(List.of(first, second));

        scheduler.publishDueTimetables();

        verify(timetableService).publishScheduled(10L);
        verify(timetableService).publishScheduled(11L);
    }
}
