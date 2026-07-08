package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "fee_categories",
       uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Data
@EqualsAndHashCode(callSuper = true)
public class FeeCategory extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, length = 200)
    private String description;
}