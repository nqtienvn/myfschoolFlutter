enum ChatMessageType { text, image, file, system }

enum ChatMessageStatus { sending, sent, delivered, read, failed }

class ChatMessage {
  const ChatMessage({
    required this.clientMessageId,
    required this.conversationId,
    required this.senderId,
    required this.senderName,
    required this.content,
    required this.type,
    required this.status,
    required this.createdAt,
    required this.isMine,
    this.id,
    this.serverSeq,
  });

  final int? id;
  final String clientMessageId;
  final int conversationId;
  final int senderId;
  final String senderName;
  final String content;
  final ChatMessageType type;
  final ChatMessageStatus status;
  final DateTime createdAt;
  final bool isMine;
  final int? serverSeq;

  ChatMessage copyWith({
    int? id,
    String? clientMessageId,
    int? conversationId,
    int? senderId,
    String? senderName,
    String? content,
    ChatMessageType? type,
    ChatMessageStatus? status,
    DateTime? createdAt,
    bool? isMine,
    int? serverSeq,
  }) {
    return ChatMessage(
      id: id ?? this.id,
      clientMessageId: clientMessageId ?? this.clientMessageId,
      conversationId: conversationId ?? this.conversationId,
      senderId: senderId ?? this.senderId,
      senderName: senderName ?? this.senderName,
      content: content ?? this.content,
      type: type ?? this.type,
      status: status ?? this.status,
      createdAt: createdAt ?? this.createdAt,
      isMine: isMine ?? this.isMine,
      serverSeq: serverSeq ?? this.serverSeq,
    );
  }
}
