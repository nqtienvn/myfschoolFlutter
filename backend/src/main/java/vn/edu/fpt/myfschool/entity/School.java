package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "schools")
@Data
@EqualsAndHashCode(callSuper = true)
public class School extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 20, unique = true)
    private String code;

    @Column(length = 500)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 200)
    private String schoolName = "FPT Schools";
}
