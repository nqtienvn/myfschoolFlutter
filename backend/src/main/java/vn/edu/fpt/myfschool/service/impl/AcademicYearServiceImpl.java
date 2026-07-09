package vn.edu.fpt.myfschool.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.AcademicYearArchiveStatsDto;
import vn.edu.fpt.myfschool.common.dto.AcademicYearDto;
import vn.edu.fpt.myfschool.common.dto.CreateAcademicYearRequest;
import vn.edu.fpt.myfschool.common.dto.UpdateAcademicYearRequest;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.SemesterStatus;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
import vn.edu.fpt.myfschool.service.AcademicYearService;

import java.time.LocalDate;
import java.util.List;

@Service("academicYearService")
@RequiredArgsConstructor
@Transactional
public class AcademicYearServiceImpl implements AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<AcademicYearDto> listAcademicYears() {
        return academicYearRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public AcademicYearDto createAcademicYear(CreateAcademicYearRequest request) {
        if (request.startDate().getYear() >= request.endDate().getYear()) {
            throw new ConflictException("Không thể tạo năm học với năm bắt đầu lớn hơn năm kết thúc");
        }
        AcademicYear year = new AcademicYear();
        String name = request.startDate().getYear() + "-" + request.endDate().getYear();
        if (academicYearRepository.existsByName(name)) {
            throw new ConflictException("Năm học đã tồn tại");
        }
        year.setName(name);
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        year.setStatus(AcademicYearStatus.DRAFT);
        AcademicYear saved = academicYearRepository.save(year);
        createDefaultSemesters(saved);
        return toDto(saved);
    }

    @Override
    public AcademicYearDto updateAcademicYear(Long id, UpdateAcademicYearRequest request) {
        AcademicYear year = findEntity(id);
        LocalDate start = request.startDate() != null ? request.startDate() : year.getStartDate();
        LocalDate end = request.endDate() != null ? request.endDate() : year.getEndDate();
        if (start.getYear() >= end.getYear()) {
            throw new ConflictException("Không thể cập nhật năm học với năm bắt đầu lớn hơn năm kết thúc");
        }
        
        String name = start.getYear() + "-" + end.getYear();
        if (!name.equals(year.getName()) && academicYearRepository.existsByName(name)) {
            throw new ConflictException("Năm học đã tồn tại");
        }

        year.setName(name);
        year.setStartDate(start);
        year.setEndDate(end);
        AcademicYear saved = academicYearRepository.save(year);

        // Cập nhật ngày bắt đầu và ngày kết thúc của các học kỳ tương ứng
        List<Semester> semesters = semesterRepository.findByAcademicYearId(id);
        for (Semester s : semesters) {
            if (s.getOrder() == 1) {
                s.setStartDate(start);
                s.setEndDate(LocalDate.of(start.getYear(), 12, 31));
            } else if (s.getOrder() == 2) {
                s.setStartDate(LocalDate.of(end.getYear(), 1, 1));
                s.setEndDate(end);
            }
            semesterRepository.save(s);
        }

        return toDto(saved);
    }

    @Override
    public AcademicYearDto updateStatus(Long id, AcademicYearStatus status) {
        AcademicYear year = findEntity(id);
        if (status == AcademicYearStatus.ACTIVE) deactivateOtherActiveYears(id);
        year.setStatus(status);
        return toDto(academicYearRepository.save(year));
    }

    @Override
    public AcademicYear findEntity(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id));
    }

    private void deactivateActiveYears() {
        academicYearRepository.findByStatus(AcademicYearStatus.ACTIVE)
                .forEach(year -> year.setStatus(AcademicYearStatus.COMPLETED));
    }

    private void deactivateOtherActiveYears(Long activeId) {
        academicYearRepository.findByStatus(AcademicYearStatus.ACTIVE).stream()
                .filter(year -> !year.getId().equals(activeId))
                .forEach(year -> year.setStatus(AcademicYearStatus.COMPLETED));
    }

    private AcademicYearDto toDto(AcademicYear year) {
        return new AcademicYearDto(year.getId(), year.getName(), year.getStartDate(), year.getEndDate(), year.getStatus());
    }

    private void createDefaultSemesters(AcademicYear year) {
        int startYear = year.getStartDate().getYear();
        int endYear = year.getEndDate().getYear();

        Semester s1 = new Semester();
        s1.setName("Học kỳ 1");
        s1.setAcademicYear(year);
        s1.setOrder(1);
        s1.setStartDate(year.getStartDate());
        s1.setEndDate(LocalDate.of(startYear, 12, 31));
        semesterRepository.save(s1);

        Semester s2 = new Semester();
        s2.setName("Học kỳ 2");
        s2.setAcademicYear(year);
        s2.setOrder(2);
        s2.setStartDate(LocalDate.of(endYear, 1, 1));
        s2.setEndDate(year.getEndDate());
        semesterRepository.save(s2);
    }

    @Override
    @Transactional
    public void openAcademicYear(Long id) {
        AcademicYear targetYear = findEntity(id);
        targetYear.setStatus(AcademicYearStatus.ACTIVE);
        academicYearRepository.save(targetYear);

        List<Semester> targetSemesters = semesterRepository.findByAcademicYearId(targetYear.getId());
        for (Semester s : targetSemesters) {
            if (s.getOrder() == 1) {
                s.setStatus(SemesterStatus.ACTIVE);
                s.setIsCurrent(true);
            } else {
                s.setStatus(SemesterStatus.NOT_STARTED);
                s.setIsCurrent(false);
            }
            semesterRepository.save(s);
        }
    }

    @Override
    @Transactional
    public void openSemester2(Long id) {
        List<Semester> semesters = semesterRepository.findByAcademicYearId(id);
        for (Semester s : semesters) {
            if (s.getOrder() == 1) {
                if (s.getStatus() == SemesterStatus.ACTIVE) {
                    s.setStatus(SemesterStatus.COMPLETED);
                }
                s.setIsCurrent(false);
                semesterRepository.save(s);
            } else if (s.getOrder() == 2) {
                s.setStatus(SemesterStatus.ACTIVE);
                s.setIsCurrent(true);
                semesterRepository.save(s);
            }
        }
    }

    @Override
    @Transactional
    public void completeAcademicYear(Long id) {
        AcademicYear year = findEntity(id);
        List<Semester> semesters = semesterRepository.findByAcademicYearId(id);
        for (Semester s : semesters) {
            if (s.getOrder() == 2) {
                s.setStatus(SemesterStatus.COMPLETED);
                s.setIsCurrent(false);
                semesterRepository.save(s);
            }
        }
        year.setStatus(AcademicYearStatus.COMPLETED);
        academicYearRepository.save(year);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicYearArchiveStatsDto getArchiveStats(Long id) {
        long classesCount = entityManager.createQuery(
                        "SELECT COUNT(c) FROM SchoolClass c WHERE c.academicYear.id = :yearId", Long.class)
                .setParameter("yearId", id).getSingleResult();

        long studentsCount = entityManager.createQuery(
                        "SELECT COUNT(DISTINCT e.student.id) FROM Enrollment e WHERE e.academicYear.id = :yearId", Long.class)
                .setParameter("yearId", id).getSingleResult();

        long teachersCount = entityManager.createQuery(
                        "SELECT COUNT(DISTINCT ta.teacher.id) FROM TeachingAssignment ta WHERE ta.cls.academicYear.id = :yearId", Long.class)
                .setParameter("yearId", id).getSingleResult();

        long parentsCount = entityManager.createQuery(
                        "SELECT COUNT(DISTINCT sg.guardian.id) FROM StudentGuardian sg JOIN Enrollment e ON e.student.id = sg.student.id WHERE e.academicYear.id = :yearId", Long.class)
                .setParameter("yearId", id).getSingleResult();

        long subjectsCount = entityManager.createQuery(
                        "SELECT COUNT(DISTINCT ta.subject.id) FROM TeachingAssignment ta WHERE ta.cls.academicYear.id = :yearId", Long.class)
                .setParameter("yearId", id).getSingleResult();

        long announcementsCount = entityManager.createQuery(
                        "SELECT COUNT(DISTINCT ac.announcement.id) FROM AnnouncementClass ac WHERE ac.cls.academicYear.id = :yearId", Long.class)
                .setParameter("yearId", id).getSingleResult();

        long chatsCount = entityManager.createQuery("SELECT COUNT(c) FROM Conversation c", Long.class).getSingleResult();
        long messagesCount = entityManager.createQuery("SELECT COUNT(m) FROM Message m", Long.class).getSingleResult();

        long attendanceCount = entityManager.createQuery(
                        "SELECT COUNT(ad) FROM AttendanceDetail ad WHERE ad.session.cls.academicYear.id = :yearId", Long.class)
                .setParameter("yearId", id).getSingleResult();

        long gradesCount = entityManager.createQuery(
                        "SELECT COUNT(ss) FROM StudentScore ss WHERE ss.gradeItem.gradeBook.semester.academicYear.id = :yearId", Long.class)
                .setParameter("yearId", id).getSingleResult();

        java.math.BigDecimal tuitionSum = entityManager.createQuery(
                        "SELECT SUM(tb.amount) FROM TuitionBill tb WHERE tb.semester.academicYear.id = :yearId AND tb.status = 'PAID'", java.math.BigDecimal.class)
                .setParameter("yearId", id).getSingleResult();
        java.math.BigDecimal tuitionCollected = tuitionSum != null ? tuitionSum : java.math.BigDecimal.ZERO;

        return new AcademicYearArchiveStatsDto(
                classesCount,
                studentsCount,
                teachersCount,
                parentsCount,
                subjectsCount,
                announcementsCount,
                messagesCount,
                chatsCount,
                attendanceCount,
                gradesCount,
                tuitionCollected
        );
    }
}
