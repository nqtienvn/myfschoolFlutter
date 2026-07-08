package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "grade_levels",
       uniqueConstraints = @UniqueConstraint(columnNames = {"code", "display_order"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class GradeLevel extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(name = "display_order", nullable = false)
    private Integer order;

    @Column(length = 200)
    private String description;
}
