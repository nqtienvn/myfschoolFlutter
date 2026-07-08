package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "periods")
@Data
@EqualsAndHashCode(callSuper = true)
public class Period extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_period_shift"))
    private SchoolShift shift;

    @Column(nullable = false)
    private Boolean isActive = true;
}
