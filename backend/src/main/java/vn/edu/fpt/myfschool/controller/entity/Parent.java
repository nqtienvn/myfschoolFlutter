package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parents")
@Data
@EqualsAndHashCode(callSuper = true)
public class Parent extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_parents_user"))
    private User user;

    @Column(length = 500)
    private String address;

    @Column(length = 200)
    private String occupation;

    @OneToMany(mappedBy = "guardian", fetch = FetchType.LAZY)
    private List<StudentGuardian> studentGuardians = new ArrayList<>();
}
