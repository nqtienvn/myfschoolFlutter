package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.AcademicYearArchiveStatsDto;
import vn.edu.fpt.myfschool.common.dto.AcademicYearDto;
import vn.edu.fpt.myfschool.common.dto.CreateAcademicYearRequest;
import vn.edu.fpt.myfschool.common.dto.UpdateAcademicYearRequest;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.entity.AcademicYear;

import java.util.List;

public interface AcademicYearService {
    List<AcademicYearDto> listAcademicYears();

    AcademicYearDto createAcademicYear(CreateAcademicYearRequest request);

    AcademicYearDto updateAcademicYear(Long id, UpdateAcademicYearRequest request);

    AcademicYearDto updateStatus(Long id, AcademicYearStatus status);

    AcademicYear findEntity(Long id);

    void openAcademicYear(Long id);

    void openSemester2(Long id);

    void completeAcademicYear(Long id);

    AcademicYearArchiveStatsDto getArchiveStats(Long id);
}
