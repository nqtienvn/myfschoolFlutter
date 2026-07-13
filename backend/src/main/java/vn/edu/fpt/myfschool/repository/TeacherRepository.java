package vn.edu.fpt.myfschool.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.entity.Teacher;

import java.util.Optional;
import java.util.List;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByUserId(Long userId);

    long countByUserStatus(UserStatus status);

    @Query("SELECT t.id FROM Teacher t WHERE t.user.status = :status")
    List<Long> findIdsByUserStatus(@Param("status") UserStatus status);

    @EntityGraph(attributePaths = {"user", "subjects"})
    @Query("SELECT DISTINCT t FROM Teacher t " +
           "JOIN t.user u " +
           "LEFT JOIN t.subjects s " +
           "WHERE (:status IS NULL OR u.status = :status) " +
           "AND (:subjectId IS NULL OR s.id = :subjectId) " +
           "AND (:keyword IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR u.phone LIKE CONCAT('%', :keyword, '%') " +
           "OR LOWER(t.employeeCode) LIKE LOWER(CONCAT('%', :keyword, '%')))" )
    Page<Teacher> searchTeachers(@Param("status") UserStatus status,
                                 @Param("keyword") String keyword,
                                 @Param("subjectId") Long subjectId,
                                 Pageable pageable);
}
