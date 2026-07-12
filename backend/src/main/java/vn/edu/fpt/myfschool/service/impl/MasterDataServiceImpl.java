package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myfschool.common.dto.PeriodDto;
import vn.edu.fpt.myfschool.common.dto.SchoolShiftDto;
import vn.edu.fpt.myfschool.entity.Period;
import vn.edu.fpt.myfschool.entity.SchoolShift;
import vn.edu.fpt.myfschool.entity.Subject;
import vn.edu.fpt.myfschool.repository.PeriodRepository;
import vn.edu.fpt.myfschool.repository.SchoolShiftRepository;
import vn.edu.fpt.myfschool.repository.SubjectRepository;
import vn.edu.fpt.myfschool.service.MasterDataService;

import java.util.List;
import java.util.Map;

@Service("masterDataService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterDataServiceImpl implements MasterDataService {

    private final SchoolShiftRepository schoolShiftRepository;
    private final PeriodRepository periodRepository;
    private final SubjectRepository subjectRepository;


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

    @Override
    @Transactional
    public Map<String, Integer> initialize() {
        SchoolShift morning = ensureShift("Sáng", "MORNING", 1);
        SchoolShift afternoon = ensureShift("Chiều", "AFTERNOON", 2);
        for (int order = 1; order <= 10; order++) {
            SchoolShift shift = order <= 5 ? morning : afternoon;
            String name = "Tiết " + order;
            if (!periodRepository.existsByNameAndShiftId(name, shift.getId())) {
                Period period = new Period();
                period.setName(name);
                period.setOrder(order);
                period.setShift(shift);
                period.setIsActive(true);
                periodRepository.save(period);
            }
        }
        ensureSubject("Toán", "TOAN");
        ensureSubject("Ngữ văn", "NGUVAN");
        ensureSubject("Vật lý", "VATLY");
        ensureSubject("Hóa học", "HOAHOC");
        ensureSubject("Sinh học", "SINHHOC");
        ensureSubject("Lịch sử", "LICHSU");
        ensureSubject("Địa lý", "DIALY");
        ensureSubject("Tiếng Anh", "TIENGANH");
        ensureSubject("Tin học", "TINHOC");
        return Map.of(
            "shifts", schoolShiftRepository.findAllByOrderByOrderAsc().size(),
            "periods", periodRepository.findAllByOrderByOrderAsc().size(),
            "subjects", subjectRepository.findAll().size());
    }

    private SchoolShift ensureShift(String name, String code, int order) {
        return schoolShiftRepository.findByCode(code).orElseGet(() -> {
            SchoolShift shift = new SchoolShift();
            shift.setName(name);
            shift.setCode(code);
            shift.setOrder(order);
            return schoolShiftRepository.save(shift);
        });
    }

    private void ensureSubject(String name, String code) {
        if (subjectRepository.existsByCode(code)) return;
        Subject subject = new Subject();
        subject.setName(name);
        subject.setCode(code);
        subjectRepository.save(subject);
    }

}
