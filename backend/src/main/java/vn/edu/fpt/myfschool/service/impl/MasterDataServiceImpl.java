package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.GradeLevelDto;
import vn.edu.fpt.myfschool.common.dto.PeriodDto;
import vn.edu.fpt.myfschool.common.dto.SchoolShiftDto;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.GradeLevel;
import vn.edu.fpt.myfschool.entity.Period;
import vn.edu.fpt.myfschool.entity.SchoolShift;
import vn.edu.fpt.myfschool.entity.Subject;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.repository.GradeLevelRepository;
import vn.edu.fpt.myfschool.repository.PeriodRepository;
import vn.edu.fpt.myfschool.repository.SchoolShiftRepository;
import vn.edu.fpt.myfschool.repository.SubjectRepository;
import vn.edu.fpt.myfschool.service.MasterDataService;

import java.time.LocalDate;
import java.util.List;

@Service("masterDataService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterDataServiceImpl implements MasterDataService {

    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolShiftRepository schoolShiftRepository;
    private final PeriodRepository periodRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SubjectRepository subjectRepository;

    @Override
    public List<GradeLevelDto> getGradeLevels() {
        return gradeLevelRepository.findAllByOrderByOrderAsc().stream()
            .map(gl -> new GradeLevelDto(
                gl.getId(),
                gl.getName(),
                gl.getCode(),
                gl.getOrder(),
                gl.getDescription()
            ))
            .toList();
    }

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
    public void initializeMasterData() {
        for (int i = 10; i <= 12; i++) {
            String code = String.valueOf(i);
            if (!gradeLevelRepository.existsByCode(code)) {
                GradeLevel gradeLevel = new GradeLevel();
                gradeLevel.setName("Khối " + i);
                gradeLevel.setCode(code);
                gradeLevel.setOrder(i);
                gradeLevel.setDescription("Khối lớp " + i + " hệ phổ thông");
                gradeLevelRepository.save(gradeLevel);
            }
        }

        SchoolShift morningShift = seedShift("Sáng", "MORNING", 1);
        SchoolShift afternoonShift = seedShift("Chiều", "AFTERNOON", 2);

        for (int i = 1; i <= 5; i++) seedPeriod("Tiết " + i, i, morningShift);
        for (int i = 6; i <= 10; i++) seedPeriod("Tiết " + i, i, afternoonShift);

        // Seed Subjects
        seedSubject("Toán", "TOAN");
        seedSubject("Vật Lý", "VATLY");
        seedSubject("Hóa", "HOAHOC");
        seedSubject("Ngữ Văn", "NGUVAN");
        seedSubject("Lịch Sử", "LICHSU");
        seedSubject("Địa Lí", "DIALY");
        seedSubject("Sinh học", "SINHHOC");
        seedSubject("Tiếng Anh", "TIENGANH");
        seedSubject("Giáo dục công dân", "GDCD");
        seedSubject("Công nghệ", "CONGNGHE");
        seedSubject("Giáo dục quốc phòng", "GDQP");
        seedSubject("Giáo dục thể chất", "GDTC");
        seedSubject("Tin học", "TINHOC");
    }

    private SchoolShift seedShift(String name, String code, Integer order) {
        return schoolShiftRepository.findByCode(code)
            .orElseGet(() -> {
                SchoolShift shift = new SchoolShift();
                shift.setName(name);
                shift.setCode(code);
                shift.setOrder(order);
                return schoolShiftRepository.save(shift);
            });
    }

    private void seedPeriod(String name, Integer order, SchoolShift shift) {
        if (!periodRepository.existsByNameAndShiftId(name, shift.getId())) {
            Period period = new Period();
            period.setName(name);
            period.setOrder(order);
            period.setShift(shift);
            period.setIsActive(true);
            periodRepository.save(period);
        }
    }

    private void seedSubject(String name, String code) {
        if (!subjectRepository.existsByCode(code)) {
            Subject s = new Subject();
            s.setName(name);
            s.setCode(code);
            subjectRepository.save(s);
        }
    }
}
