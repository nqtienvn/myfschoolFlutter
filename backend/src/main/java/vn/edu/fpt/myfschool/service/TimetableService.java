package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.CreateTimetableRequest;
import vn.edu.fpt.myfschool.common.dto.TimetableDto;

import java.time.LocalDate;
import java.util.List;

public interface TimetableService {
    List<TimetableDto> list(Long classId, Long semesterId);
    TimetableDto get(Long id);
    TimetableDto createDraft(CreateTimetableRequest request);
    TimetableDto publish(Long id, LocalDate effectiveFrom);
    TimetableDto schedulePublish(Long id, LocalDate publishDate);
    TimetableDto publishScheduled(Long id);
    void deleteDraft(Long id);
}
