package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myfschool.common.dto.PeriodDto;
import vn.edu.fpt.myfschool.common.dto.SchoolShiftDto;
import vn.edu.fpt.myfschool.entity.Period;
import vn.edu.fpt.myfschool.repository.PeriodRepository;
import vn.edu.fpt.myfschool.repository.SchoolShiftRepository;
import vn.edu.fpt.myfschool.service.MasterDataService;

import java.util.List;

@Service("masterDataService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterDataServiceImpl implements MasterDataService {

    private final SchoolShiftRepository schoolShiftRepository;
    private final PeriodRepository periodRepository;


    @Override
    public List<SchoolShiftDto> getShifts() {
        return schoolShiftRepository.findAllByOrderByOrderAsc().stream()
            .map(ss -> new SchoolShiftDto(
                ss.getId(),
                ss.getName(),
                ss.getCode(),
                ss.getOrder()
            ))
            .toList();
    }

    @Override
    public List<PeriodDto> getPeriods(Long shiftId) {
        List<Period> periods = (shiftId != null)
            ? periodRepository.findByShiftIdOrderByOrderAsc(shiftId)
            : periodRepository.findAllByOrderByOrderAsc();

        return periods.stream()
            .map(p -> new PeriodDto(
                p.getId(),
                p.getName(),
                p.getOrder(),
                p.getShift().getId(),
                p.getShift().getName()
            ))
            .toList();
    }

}
