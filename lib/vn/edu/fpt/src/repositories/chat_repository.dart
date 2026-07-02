import '../api/client/chat_api_client.dart';
import '../models/chat_message.dart';
import '../models/conversation.dart';

class ChatRepository {
  const ChatRepository({required ChatApiClient apiClient}) : _apiClient = apiClient;

  final ChatApiClient _apiClient;

  Future<List<Conversation>> getConversations({required String token}) async {
    final dtos = await _apiClient.getConversations(token: token);
    return dtos.map((dto) => dto.toDomain()).toList();
  }

  Future<Conversation> createConversation({required String token, required int otherUserId}) async {
    final dto = await _apiClient.createConversation(token: token, otherUserId: otherUserId);
    return dto.toDomain();
  }

  Future<List<ChatMessage>> getMessages({
    required String token,
    required int currentUserId,
    required int conversationId,
    int? beforeMessageId,
    int? afterSeq,
    int limit = 20,
  }) async {
    final dtos = await _apiClient.getMessages(
      token: token,
      conversationId: conversationId,
      beforeMessageId: beforeMessageId,
      afterSeq: afterSeq,
      limit: limit,
    );
    final messages = dtos.map((dto) => dto.toDomain(currentUserId)).toList();
    messages.sort(_compareMessages);
    return messages;
  }

  Future<void> markRead({required String token, required int conversationId, int? lastReadMessageId}) {
    return _apiClient.markRead(token: token, conversationId: conversationId, lastReadMessageId: lastReadMessageId);
  }

  Future<int> getUnreadCount({required String token}) => _apiClient.getUnreadCount(token: token);

  int _compareMessages(ChatMessage a, ChatMessage b) {
    final seqA = a.serverSeq;
    final seqB = b.serverSeq;
    if (seqA != null && seqB != null) return seqA.compareTo(seqB);
    return a.createdAt.compareTo(b.createdAt);
  }
}
