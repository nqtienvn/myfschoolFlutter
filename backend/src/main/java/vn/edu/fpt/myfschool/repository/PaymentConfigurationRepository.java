package vn.edu.fpt.myfschool.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.PaymentConfiguration;

@Repository
public interface PaymentConfigurationRepository
        extends JpaRepository<PaymentConfiguration, Long> {

    Optional<PaymentConfiguration> findByAcademicYearId(Long academicYearId);

    Optional<PaymentConfiguration> findByAcademicYearIdAndEnabledTrue(Long academicYearId);
}
