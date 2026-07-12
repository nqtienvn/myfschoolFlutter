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
import vn.edu.fpt.myfschool.service.AcademicYearReadinessService;

import java.time.LocalDate;
import java.util.List;

@Service("academicYearService")
@RequiredArgsConstructor
@Transactional
public class AcademicYearServiceImpl implements AcademicYearService {
    private final vn.edu.fpt.myfschool.service.GradeConfigurationService gradeConfigurationService;

    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final EntityManager entityManager;
    private final AcademicYearReadinessService readinessService;

    @Override
    @Transactional(readOnly = true)
    public List<AcademicYearDto> listAcademicYears() {
        return academicYearRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public AcademicYearDto createAcademicYear(CreateAcademicYearRequest request) {
        requireConsecutiveYears(request.startDate(), request.endDate());
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
        gradeConfigurationService.copyToYear(saved.getId(), request.gradeConfigTemplateId(), request.gradeConfigItems());
        return toDto(saved);
    }

    @Override
    public AcademicYearDto updateAcademicYear(Long id, UpdateAcademicYearRequest request) {
        AcademicYear year = findEntity(id);
        if (year.getStatus() != AcademicYearStatus.DRAFT) {
            throw new ConflictException("Chỉ được sửa năm học ở trạng thái DRAFT");
        }
        LocalDate start = request.startDate() != null ? request.startDate() : year.getStartDate();
        LocalDate end = request.endDate() != null ? request.endDate() : year.getEndDate();
        requireConsecutiveYears(start, end);
        
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
        if (status == AcademicYearStatus.ACTIVE) {
            openAcademicYear(id);
            return toDto(findEntity(id));
        }
        if (status == AcademicYearStatus.COMPLETED) {
            completeAcademicYear(id);
            return toDto(findEntity(id));
        }
        if (status == AcademicYearStatus.DRAFT && year.getStatus() == AcademicYearStatus.DRAFT) return toDto(year);
        throw new ConflictException("Không được chuyển lùi trạng thái năm học");
    }

    @Override
    public AcademicYear findEntity(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id));
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

    private void requireConsecutiveYears(LocalDate startDate, LocalDate endDate) {
        if (endDate.getYear() != startDate.getYear() + 1) {
            throw new ConflictException("Năm học phải nằm trong hai năm liên tiếp, ví dụ 2024-2025");
        }
    }

    @Override
    @Transactional
    public void openAcademicYear(Long id) {
        AcademicYear targetYear = findEntity(id);
        if (targetYear.getStatus() != AcademicYearStatus.DRAFT) {
            throw new ConflictException("Chỉ năm học DRAFT mới được kích hoạt");
        }
        if (academicYearRepository.findByStatus(AcademicYearStatus.ACTIVE).stream()
                .anyMatch(year -> !year.getId().equals(id))) {
            throw new ConflictException("Đang có một năm học ACTIVE; hãy hoàn tất năm học đó trước");
        }
        readinessService.requireReady(id);
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
        AcademicYear year = findEntity(id);
        if (year.getStatus() != AcademicYearStatus.ACTIVE) {
            throw new ConflictException("Năm học phải ACTIVE mới được mở Học kỳ 2");
        }
        List<Semester> semesters = semesterRepository.findByAcademicYearId(id);
        Semester semester1 = semesters.stream().filter(item -> item.getOrder() == 1).findFirst()
            .orElseThrow(() -> new ConflictException("Thiếu Học kỳ 1"));
        Semester semester2 = semesters.stream().filter(item -> item.getOrder() == 2).findFirst()
            .orElseThrow(() -> new ConflictException("Thiếu Học kỳ 2"));
        if (semester1.getStatus() != SemesterStatus.ACTIVE || semester2.getStatus() != SemesterStatus.NOT_STARTED) {
            throw new ConflictException("Trạng thái học kỳ không hợp lệ để mở Học kỳ 2");
        }
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
        if (year.getStatus() != AcademicYearStatus.ACTIVE) {
            throw new ConflictException("Chỉ năm học ACTIVE mới được hoàn tất");
        }
        List<Semester> semesters = semesterRepository.findByAcademicYearId(id);
        Semester semester2 = semesters.stream().filter(item -> item.getOrder() == 2).findFirst()
            .orElseThrow(() -> new ConflictException("Thiếu Học kỳ 2"));
        if (semester2.getStatus() != SemesterStatus.ACTIVE) {
            throw new ConflictException("Học kỳ 2 phải ACTIVE trước khi hoàn tất năm học");
        }
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
