import '../dto/chat_message_dto.dart';
import '../dto/conversation_dto.dart';
import '../dto/search_result_dto.dart';
import '../exception/parse_exception.dart';
import 'backend_api_client.dart';

class ChatApiClient {
  const ChatApiClient({required BackendApiClient backend}) : _backend = backend;

  final BackendApiClient _backend;

  Future<List<ConversationDto>> getConversations({required String token}) async {
    final data = await _backend.getData('/api/conversations', token: token);
    return _asList(data).map((item) => ConversationDto.fromJson(item)).toList();
  }

  Future<ConversationDto> createConversation({required String token, required int otherUserId}) async {
    final data = await _backend.postData(
      '/api/conversations',
      token: token,
      body: {'otherUserId': otherUserId},
    );
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Conversation response data must be object.');
    }
    return ConversationDto.fromJson(data);
  }

  Future<List<ChatMessageDto>> getMessages({
    required String token,
    required int conversationId,
    int? beforeMessageId,
    int? afterSeq,
    int limit = 20,
  }) async {
    final data = await _backend.getData(
      '/api/conversations/$conversationId',
      token: token,
      query: {
        'beforeMessageId': beforeMessageId?.toString(),
        'afterSeq': afterSeq?.toString(),
        'limit': limit.toString(),
      },
    );
    return _asList(data).map((item) => ChatMessageDto.fromJson(item)).toList();
  }

  Future<void> markRead({required String token, required int conversationId, int? lastReadMessageId}) async {
    await _backend.putData(
      '/api/conversations/$conversationId/read',
      token: token,
      body: lastReadMessageId == null ? null : {'lastReadMessageId': lastReadMessageId},
    );
  }

  Future<int> getUnreadCount({required String token}) async {
    final data = await _backend.getData('/api/conversations/unread-count', token: token);
    if (data is! int) throw const ParseException('Unread count must be int.');
    return data;
  }

  Future<List<SearchResultDto>> searchUsers({required String token, required String keyword}) async {
    final data = await _backend.getData(
      '/api/conversations/search-users',
      token: token,
      query: {'keyword': keyword},
    );
    return _asList(data).map((item) => SearchResultDto.fromJson(item)).toList();
  }

  List<Map<String, dynamic>> _asList(Object? data) {
    if (data is! List) throw const ParseException('Response data must be list.');
    return data.map((item) {
      if (item is! Map<String, dynamic>) {
        throw const ParseException('List item must be object.');
      }
      return item;
    }).toList();
  }
}
