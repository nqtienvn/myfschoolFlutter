package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.ClubRegistration;
import java.util.List;

@Repository
public interface ClubRegistrationRepository extends JpaRepository<ClubRegistration, Long> {
    List<ClubRegistration> findByStudentIdAndAcademicYearOrderByRegisteredAtDesc(Long studentId, String academicYear);
    boolean existsByStudentIdAndClubNameAndAcademicYear(Long studentId, String clubName, String academicYear);

    @Query("SELECT cr FROM ClubRegistration cr WHERE cr.clubName = :clubName " +
           "AND cr.academicYear = :year AND cr.status = 'REGISTERED'")
    List<ClubRegistration> findActiveByClub(@Param("clubName") String clubName, @Param("year") String year);
}
