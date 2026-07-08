package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.controller.entity.Subject;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT s FROM Subject s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Subject> search(@Param("keyword") String keyword);
}
