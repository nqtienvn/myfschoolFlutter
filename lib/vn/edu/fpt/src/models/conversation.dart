class ChatParticipant {
  const ChatParticipant({
    required this.userId,
    required this.name,
    required this.role,
  });

  final int userId;
  final String name;
  final String role;
}

class Conversation {
  const Conversation({
    required this.id,
    required this.unreadCount,
    this.lastMessage,
    this.lastMessageAt,
    this.otherParticipant,
    this.isOnline = false,
    this.lastSeenAt,
  });

  final int id;
  final String? lastMessage;
  final DateTime? lastMessageAt;
  final int unreadCount;
  final ChatParticipant? otherParticipant;
  final bool isOnline;
  final DateTime? lastSeenAt;

  Conversation copyWith({
    String? lastMessage,
    DateTime? lastMessageAt,
    int? unreadCount,
    ChatParticipant? otherParticipant,
    bool? isOnline,
    DateTime? lastSeenAt,
  }) {
    return Conversation(
      id: id,
      lastMessage: lastMessage ?? this.lastMessage,
      lastMessageAt: lastMessageAt ?? this.lastMessageAt,
      unreadCount: unreadCount ?? this.unreadCount,
      otherParticipant: otherParticipant ?? this.otherParticipant,
      isOnline: isOnline ?? this.isOnline,
      lastSeenAt: lastSeenAt ?? this.lastSeenAt,
    );
  }
}
