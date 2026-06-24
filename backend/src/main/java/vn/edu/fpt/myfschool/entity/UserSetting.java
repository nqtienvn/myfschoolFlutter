package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.Language;
import vn.edu.fpt.myfschool.common.enums.Theme;

@Entity
@Table(name = "user_settings")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserSetting extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_us_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Theme theme = Theme.LIGHT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language = Language.VI;

    @Column(nullable = false)
    private Boolean notificationEnabled = true;
}
