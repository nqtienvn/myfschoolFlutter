package vn.edu.fpt.myfschool.service;


import vn.edu.fpt.myfschool.common.dto.PeriodDto;
import vn.edu.fpt.myfschool.common.dto.SchoolShiftDto;

import java.util.List;

public interface MasterDataService {

    List<SchoolShiftDto> getShifts();
    List<PeriodDto> getPeriods(Long shiftId);
}
