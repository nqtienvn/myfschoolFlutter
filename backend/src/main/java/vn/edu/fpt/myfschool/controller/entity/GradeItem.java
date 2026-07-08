package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "grade_items",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"grade_book_id", "name"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class GradeItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_book_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_gi_gradebook"))
    private GradeBook gradeBook;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer weight;

    @Column(nullable = false)
    private Integer maxScore = 10;

    @Column(name = "display_order", nullable = false)
    private Integer order = 0;
}