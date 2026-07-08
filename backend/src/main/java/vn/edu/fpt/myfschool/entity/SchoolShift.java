package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "school_shifts")
@Data
@EqualsAndHashCode(callSuper = true)
public class SchoolShift extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Column(name = "display_order", nullable = false)
    private Integer order;
}
