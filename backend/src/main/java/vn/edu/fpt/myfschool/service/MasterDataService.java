package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.GradeLevelDto;
import vn.edu.fpt.myfschool.common.dto.PeriodDto;
import vn.edu.fpt.myfschool.common.dto.SchoolShiftDto;

import java.util.List;

public interface MasterDataService {
    List<GradeLevelDto> getGradeLevels();
    List<SchoolShiftDto> getShifts();
    List<PeriodDto> getPeriods(Long shiftId);
    void initializeMasterData();
}
