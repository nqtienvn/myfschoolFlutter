import '../../models/chat_message.dart';
import '../exception/parse_exception.dart';

class ChatMessageDto {
  const ChatMessageDto({
    required this.id,
    required this.clientMessageId,
    required this.conversationId,
    required this.senderId,
    required this.senderName,
    required this.senderAvatar,
    required this.messageType,
    required this.content,
    required this.serverSeq,
    required this.status,
    required this.createdAt,
  });

  final int? id;
  final String clientMessageId;
  final int conversationId;
  final int senderId;
  final String senderName;
  final String? senderAvatar;
  final String messageType;
  final String content;
  final int? serverSeq;
  final String status;
  final DateTime createdAt;

  factory ChatMessageDto.fromJson(Map<String, dynamic> json) {
    return ChatMessageDto(
      id: json['id'] as int?,
      clientMessageId: json['clientMessageId'] is String ? json['clientMessageId'] as String : '',
      conversationId: requireField<int>(json, 'conversationId'),
      senderId: requireField<int>(json, 'senderId'),
      senderName: requireField<String>(json, 'senderName'),
      senderAvatar: json['senderAvatar'] as String?,
      messageType: json['messageType'] is String ? json['messageType'] as String : 'TEXT',
      content: requireField<String>(json, 'content'),
      serverSeq: json['serverSeq'] as int?,
      status: json['status'] is String ? json['status'] as String : 'sent',
      createdAt: DateTime.parse(requireField<String>(json, 'createdAt')),
    );
  }

  ChatMessage toDomain(int currentUserId) {
    return ChatMessage(
      id: id,
      clientMessageId: clientMessageId,
      conversationId: conversationId,
      senderId: senderId,
      senderName: senderName,
      senderAvatar: senderAvatar,
      content: content,
      type: _parseType(messageType),
      status: _parseStatus(status),
      createdAt: createdAt,
      isMine: senderId == currentUserId,
      serverSeq: serverSeq,
    );
  }

  static ChatMessageType _parseType(String value) {
    switch (value.toUpperCase()) {
      case 'IMAGE':
        return ChatMessageType.image;
      case 'FILE':
        return ChatMessageType.file;
      case 'SYSTEM':
        return ChatMessageType.system;
      case 'TEXT':
      default:
        return ChatMessageType.text;
    }
  }

  static ChatMessageStatus _parseStatus(String value) {
    switch (value.toLowerCase()) {
      case 'delivered':
        return ChatMessageStatus.delivered;
      case 'read':
        return ChatMessageStatus.read;
      case 'failed':
        return ChatMessageStatus.failed;
      case 'sending':
        return ChatMessageStatus.sending;
      case 'sent':
      default:
        return ChatMessageStatus.sent;
    }
  }
}
