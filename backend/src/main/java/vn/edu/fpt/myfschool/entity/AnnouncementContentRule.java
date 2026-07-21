package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import vn.edu.fpt.myfschool.common.enums.AnnouncementPolicyMatchType;
import vn.edu.fpt.myfschool.common.enums.AnnouncementPolicyScope;

@Entity
@Table(name = "announcement_content_rules",
        uniqueConstraints = @UniqueConstraint(name = "uk_announcement_rule",
                columnNames = {"academic_year_id", "normalized_phrase", "match_scope", "match_type"}))
@Getter
@Setter
public class AnnouncementContentRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(nullable = false, length = 250)
    private String phrase;

    @Column(name = "normalized_phrase", nullable = false, length = 250)
    private String normalizedPhrase;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_scope", nullable = false, length = 12)
    private AnnouncementPolicyScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 12)
    private AnnouncementPolicyMatchType matchType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
}
