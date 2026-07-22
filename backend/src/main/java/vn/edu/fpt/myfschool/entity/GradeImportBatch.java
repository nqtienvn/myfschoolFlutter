package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "grade_import_batches")
@Getter
@Setter
@NoArgsConstructor
public class GradeImportBatch extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_gib_year"))
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_gib_semester"))
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_item_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_gib_config_item"))
    private AcademicYearGradeConfigItem configItem;

    @Column(nullable = false)
    private Integer itemOccurrence;

    @Column(nullable = false, length = 50)
    private String itemCode;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 64)
    private String fileHash;

    @Column(nullable = false, length = 20)
    private String status = "COMPLETED";

    @Column(nullable = false)
    private Integer totalRows = 0;

    @Column(nullable = false)
    private Integer updatedScores = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by", nullable = false,
            foreignKey = @ForeignKey(name = "fk_gib_imported_by"))
    private User importedBy;
}
