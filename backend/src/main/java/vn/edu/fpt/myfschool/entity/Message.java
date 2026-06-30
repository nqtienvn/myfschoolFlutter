package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import vn.edu.fpt.myfschool.common.enums.MessageType;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "messages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_message_sender_client",
                        columnNames = {"sender_id", "client_message_id"} //1 sender id cso 1 client message id
                ),
                @UniqueConstraint(
                        name = "uk_message_conversation_seq",
                        columnNames = {"conversation_id", "server_seq"} //1 conversation với 1 thứ tự
                )
        },
        indexes = {
                @Index(name = "idx_message_conversation_created", columnList = "conversation_id, created_at"),
                @Index(name = "idx_message_conversation_seq", columnList = "conversation_id, server_seq")
        })
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_msg_conversation"))
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_msg_sender"))
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attachment> attachments = new ArrayList<>();

    @Column(length = 80)
    private String clientMessageId; //để chống gửi trùng tin nhắn
    //khi fe gửi tin nhắn qua ws nó tạo ra một mã tạm là clientId
    //khi lưu db thành công nhưng mang lag nên fe chưa nhận được thì fe tưởng fail và retry lại cùng message
    //nếu Be không kiểm tra db có thể gửi 2 tin nhắn giống nhau
    //nên cần thêm unique để khi cùng 1 sender cùng 1 cliend mesageid thì chỉ luu db 1 lần


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType messageType = MessageType.TEXT;

    @Column(nullable = false)
    private Long serverSeq;
//serverSeq là số thứ tự tăng dần của message trong từng conversation do be cấp
//để Fe biết mình đã sync đến message số mấy
//để sắp xếp cuộc hồi thoại
    //phản ảnh chính xác thứ tự tin lưu nào be để trả về fe chuẩn xác nhất
}

