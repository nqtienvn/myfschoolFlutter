package vn.edu.fpt.myfschool.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import vn.edu.fpt.myfschool.entity.GradeLevel;
import vn.edu.fpt.myfschool.entity.Period;
import vn.edu.fpt.myfschool.entity.School;
import vn.edu.fpt.myfschool.entity.SchoolShift;
import vn.edu.fpt.myfschool.repository.GradeLevelRepository;
import vn.edu.fpt.myfschool.repository.PeriodRepository;
import vn.edu.fpt.myfschool.repository.SchoolRepository;
import vn.edu.fpt.myfschool.repository.SchoolShiftRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class MasterDataSeeder implements CommandLineRunner {

    private final SchoolRepository schoolRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolShiftRepository schoolShiftRepository;
    private final PeriodRepository periodRepository;

    @Override
    public void run(String... args) {
        log.info("Bắt đầu kiểm tra và seed dữ liệu Master Data...");

        // 1. Seed School
        if (!schoolRepository.existsByCode("FPT")) {
            School school = new School();
            school.setName("FPT Schools");
            school.setCode("FPT");
            school.setAddress("Khu Công nghệ cao Hòa Lạc, Thạch Thất, Hà Nội");
            school.setPhone("02473007300");
            school.setSchoolName("FPT Schools");
            schoolRepository.save(school);
            log.info("Đã tạo cơ sở trường học mặc định: FPT Schools");
        }

        // 2. Seed GradeLevels (Khối 1 - 12)
        for (int i = 1; i <= 12; i++) {
            String code = String.valueOf(i);
            if (!gradeLevelRepository.existsByCode(code)) {
                GradeLevel gl = new GradeLevel();
                gl.setName("Khối " + i);
                gl.setCode(code);
                gl.setOrder(i);
                gl.setDescription("Khối lớp " + i + " hệ phổ thông");
                gradeLevelRepository.save(gl);
                log.info("Đã tạo Khối lớp: {}", gl.getName());
            }
        }

        // 3. Seed SchoolShifts (Ca Sáng / Ca Chiều)
        SchoolShift morningShift = seedShift("Sáng", "MORNING", 1);
        SchoolShift afternoonShift = seedShift("Chiều", "AFTERNOON", 2);

        // 4. Seed Periods (Tiết 1 - 5 ca Sáng, Tiết 6 - 10 ca Chiều)
        if (morningShift != null) {
            for (int i = 1; i <= 5; i++) {
                seedPeriod("Tiết " + i, i, morningShift);
            }
        }
        if (afternoonShift != null) {
            for (int i = 6; i <= 10; i++) {
                seedPeriod("Tiết " + i, i, afternoonShift);
            }
        }

        log.info("Hoàn tất kiểm tra và seed dữ liệu Master Data!");
    }

    private SchoolShift seedShift(String name, String code, Integer order) {
        if (!schoolShiftRepository.existsByCode(code)) {
            SchoolShift shift = new SchoolShift();
            shift.setName(name);
            shift.setCode(code);
            shift.setOrder(order);
            SchoolShift saved = schoolShiftRepository.save(shift);
            log.info("Đã tạo ca học: {}", name);
            return saved;
        }
        return schoolShiftRepository.findByCode(code).orElse(null);
    }

    private void seedPeriod(String name, Integer order, SchoolShift shift) {
        if (!periodRepository.existsByNameAndShiftId(name, shift.getId())) {
            Period p = new Period();
            p.setName(name);
            p.setOrder(order);
            p.setShift(shift);
            p.setIsActive(true);
            periodRepository.save(p);
            log.info("Đã tạo tiết học: {} thuộc {}", name, shift.getName());
        }
    }
}
