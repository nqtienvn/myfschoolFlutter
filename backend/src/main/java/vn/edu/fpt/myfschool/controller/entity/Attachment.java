package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "attachments")
@Data
@EqualsAndHashCode(callSuper = true)
public class Attachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_request_id",
                foreignKey = @ForeignKey(name = "fk_att_lr"))
    private LeaveRequest leaveRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id",
                foreignKey = @ForeignKey(name = "fk_att_msg"))
    private Message message;

    @Column(nullable = false, length = 500)
    private String fileUrl;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false)
    private Integer fileSize;

    @Column(nullable = false, length = 100)
    private String mimeType;
}
