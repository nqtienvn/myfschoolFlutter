package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.fpt.myfschool.common.enums.*;

@Entity @Table(name="academic_year_grade_config_items", uniqueConstraints=@UniqueConstraint(columnNames={"config_id","code"}))
@Getter @Setter @NoArgsConstructor
public class AcademicYearGradeConfigItem extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="config_id",nullable=false) private AcademicYearGradeConfig config;
    @Column(nullable=false,length=40) private String code;
    @Column(nullable=false,length=100) private String displayName;
    @Column(nullable=false) private Integer weight;
    @Column(nullable=false) private Integer quantity;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private GradeEntryRole entryRole;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private AssessmentType assessmentType = AssessmentType.SCORE;
    @Column(nullable=false) private Boolean requiredEntry = true;
    @Column(nullable=false) private Integer displayOrder;
}
