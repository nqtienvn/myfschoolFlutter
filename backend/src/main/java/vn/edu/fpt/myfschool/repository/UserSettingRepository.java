package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.controller.entity.UserSetting;
import java.util.Optional;

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    Optional<UserSetting> findByUserId(Long userId);
}
