package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "announcement_policy_violations")
@Getter
@Setter
public class AnnouncementPolicyViolation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private AnnouncementContentRule rule;

    @Column(name = "matched_field", nullable = false, length = 12)
    private String matchedField;

    @Column(name = "matched_phrase", nullable = false, length = 250)
    private String matchedPhrase;
}
