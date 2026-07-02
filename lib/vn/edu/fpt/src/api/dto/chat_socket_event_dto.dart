import 'chat_message_dto.dart';
import '../exception/parse_exception.dart';

class ChatSocketEventDto {
  const ChatSocketEventDto({
    required this.type,
    this.clientMessageId,
    this.status,
    this.message,
    this.conversationId,
    this.messageId,
    this.userId,
    this.lastReadMessageId,
    this.typing,
    this.online,
    this.lastSeenAt,
    this.errorCode,
    this.errorMessage,
  });

  final String type;
  final String? clientMessageId;
  final String? status;
  final ChatMessageDto? message;
  final int? conversationId;
  final int? messageId;
  final int? userId;
  final int? lastReadMessageId;
  final bool? typing;
  final bool? online;
  final DateTime? lastSeenAt;
  final String? errorCode;
  final String? errorMessage;

  factory ChatSocketEventDto.fromJson(Map<String, dynamic> json) {
    final type = requireField<String>(json, 'type');
    final messageJson = json['message'];
    return ChatSocketEventDto(
      type: type,
      clientMessageId: json['clientMessageId'] as String?,
      status: json['status'] as String?,
      message: messageJson is Map<String, dynamic> ? ChatMessageDto.fromJson(messageJson) : null,
      conversationId: json['conversationId'] as int?,
      messageId: json['messageId'] as int?,
      userId: json['userId'] as int?,
      lastReadMessageId: json['lastReadMessageId'] as int?,
      typing: json['typing'] as bool?,
      online: json['online'] as bool?,
      lastSeenAt: json['lastSeenAt'] is String ? DateTime.parse(json['lastSeenAt'] as String) : null,
      errorCode: json['code'] as String?,
      errorMessage: json['message'] is String ? json['message'] as String : null,
    );
  }
}
