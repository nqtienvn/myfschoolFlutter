package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
