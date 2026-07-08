package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "rooms")
@Data
@EqualsAndHashCode(callSuper = true)
public class Room extends BaseEntity {

    @Column(nullable = false, length = 20, unique = true)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @Column(length = 20)
    private String building;

    @Column(length = 100)
    private String equipment;

    @Column(nullable = false)
    private Boolean isActive = true;
}
