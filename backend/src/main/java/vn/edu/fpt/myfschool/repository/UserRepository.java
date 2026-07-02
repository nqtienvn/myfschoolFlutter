package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    List<User> findByRole(UserRole role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "u.phone LIKE CONCAT('%', :keyword, '%'))")
    List<User> searchByRoleAndKeyword(@Param("role") UserRole role,
                                       @Param("keyword") String keyword);

    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "u.phone LIKE CONCAT('%', :keyword, '%')")
    List<User> searchByKeyword(@Param("keyword") String keyword);
}
