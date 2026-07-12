package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity @Table(name="grade_config_templates", uniqueConstraints=@UniqueConstraint(columnNames={"name","version"}))
@Getter @Setter @NoArgsConstructor
public class GradeConfigTemplate extends BaseEntity {
    @Column(nullable=false,length=120) private String name;
    @Column(nullable=false) private Integer version = 1;
    @Column(nullable=false) private Boolean active = true;
    @OneToMany(mappedBy="template",cascade=CascadeType.ALL,orphanRemoval=true)
    @OrderBy("displayOrder asc") private List<GradeConfigTemplateItem> items = new ArrayList<>();
}
