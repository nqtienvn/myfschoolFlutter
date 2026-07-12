package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity @Table(name="academic_year_grade_configs") @Getter @Setter @NoArgsConstructor
public class AcademicYearGradeConfig extends BaseEntity {
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="academic_year_id",nullable=false,unique=true) private AcademicYear academicYear;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="source_template_id") private GradeConfigTemplate sourceTemplate;
    @Column(nullable=false,length=20) private String status = "VALIDATED";
    @OneToMany(mappedBy="config",cascade=CascadeType.ALL,orphanRemoval=true)
    @OrderBy("displayOrder asc") private List<AcademicYearGradeConfigItem> items = new ArrayList<>();
}
