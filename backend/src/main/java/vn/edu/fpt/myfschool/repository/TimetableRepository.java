package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.myfschool.common.enums.TimetableStatus;
import vn.edu.fpt.myfschool.entity.Timetable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    List<Timetable> findByClsIdAndSemesterIdOrderByVersionDesc(Long classId, Long semesterId);
    Optional<Timetable> findFirstByClsIdAndSemesterIdAndStatusOrderByVersionDesc(
        Long classId, Long semesterId, TimetableStatus status);
    Optional<Timetable> findFirstByClsIdAndSemesterIdOrderByVersionDesc(Long classId, Long semesterId);
    boolean existsByClsIdAndSemesterIdAndStatus(Long classId, Long semesterId, TimetableStatus status);
    List<Timetable> findByStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromAsc(
        TimetableStatus status, LocalDate effectiveFrom);

    @Query("SELECT t FROM Timetable t WHERE t.cls.id = :classId AND t.semester.id = :semesterId " +
        "AND t.status IN ('ACTIVE', 'ARCHIVED') AND t.effectiveFrom <= :date " +
        "AND (t.effectiveTo IS NULL OR t.effectiveTo >= :date) ORDER BY t.version DESC")
    List<Timetable> findEffective(@Param("classId") Long classId,
                                  @Param("semesterId") Long semesterId,
                                  @Param("date") LocalDate date);

    @Query("SELECT t FROM Timetable t WHERE t.semester.id = :semesterId " +
        "AND t.status IN ('ACTIVE', 'ARCHIVED') AND t.effectiveFrom <= :date " +
        "AND (t.effectiveTo IS NULL OR t.effectiveTo >= :date)")
    List<Timetable> findEffectiveBySemester(@Param("semesterId") Long semesterId,
                                            @Param("date") LocalDate date);

    @Query("SELECT t FROM Timetable t WHERE t.semester.id = :semesterId " +
        "AND t.status IN ('ACTIVE', 'ARCHIVED', 'SCHEDULED') " +
        "AND t.effectiveFrom <= :toDate AND (t.effectiveTo IS NULL OR t.effectiveTo >= :fromDate)")
    List<Timetable> findOverlappingPublished(@Param("semesterId") Long semesterId,
                                             @Param("fromDate") LocalDate fromDate,
                                             @Param("toDate") LocalDate toDate);
}
