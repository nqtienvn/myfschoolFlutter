import '../../models/conversation.dart';
import '../exception/parse_exception.dart';

class ChatParticipantDto {
  const ChatParticipantDto({
    required this.userId,
    required this.name,
    required this.role,
  });

  final int userId;
  final String name;
  final String role;

  factory ChatParticipantDto.fromJson(Map<String, dynamic> json) {
    return ChatParticipantDto(
      userId: requireField<int>(json, 'userId'),
      name: requireField<String>(json, 'name'),
      role: requireField<String>(json, 'role'),
    );
  }

  ChatParticipant toDomain() =>
      ChatParticipant(userId: userId, name: name, role: role);
}

class ConversationDto {
  const ConversationDto({
    required this.id,
    required this.unreadCount,
    this.lastMessage,
    this.lastMessageAt,
    this.otherParticipant,
  });

  final int id;
  final String? lastMessage;
  final DateTime? lastMessageAt;
  final int unreadCount;
  final ChatParticipantDto? otherParticipant;

  factory ConversationDto.fromJson(Map<String, dynamic> json) {
    final participantJson = json['otherParticipant'];
    return ConversationDto(
      id: requireField<int>(json, 'id'),
      lastMessage: json['lastMessage'] as String?,
      lastMessageAt: json['lastMessageAt'] is String
          ? DateTime.parse(json['lastMessageAt'] as String)
          : null,
      unreadCount: json['unreadCount'] is int ? json['unreadCount'] as int : 0,
      otherParticipant: participantJson is Map<String, dynamic>
          ? ChatParticipantDto.fromJson(participantJson)
          : null,
    );
  }

  Conversation toDomain() => Conversation(
    id: id,
    lastMessage: lastMessage,
    lastMessageAt: lastMessageAt,
    unreadCount: unreadCount,
    otherParticipant: otherParticipant?.toDomain(),
  );
}
