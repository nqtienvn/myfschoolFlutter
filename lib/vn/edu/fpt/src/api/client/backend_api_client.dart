import 'dart:convert';
import 'dart:io';

import '../dto/api_response.dart';
import '../exception/backend_api_exception.dart';

class BackendApiClient {
  BackendApiClient({String? baseUrl})
      : baseUri = Uri.parse(
          baseUrl ??
              const String.fromEnvironment(
                'API_BASE_URL',
                defaultValue: 'http://10.0.2.2:8080',
              ),
        );

  final Uri baseUri;
  final HttpClient _client = HttpClient();

  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) {
    return _send('GET', path, token: token, query: query);
  }

  Future<Object?> postData(String path, {String? token, Object? body}) {
    return _send('POST', path, token: token, body: body);
  }

  Future<Object?> putData(String path, {String? token, Object? body}) {
    return _send('PUT', path, token: token, body: body);
  }

  Uri wsUri(String path, {required String token}) {
    return baseUri.replace(
      scheme: baseUri.scheme == 'https' ? 'wss' : 'ws',
      path: path,
      queryParameters: {'token': token},
    );
  }

  Future<Object?> _send(
    String method,
    String path, {
    String? token,
    Map<String, String?> query = const {},
    Object? body,
  }) async {
    final uri = baseUri.replace(
      path: path,
      queryParameters: {
        for (final entry in query.entries)
          if (entry.value != null) entry.key: entry.value!,
      },
    );
    final request = await _client.openUrl(method, uri);
    request.headers.set(HttpHeaders.acceptHeader, 'application/json');
    request.headers.set(HttpHeaders.contentTypeHeader, 'application/json; charset=utf-8');
    if (token != null) {
      request.headers.set(HttpHeaders.authorizationHeader, 'Bearer $token');
    }
    if (body != null) {
      request.write(jsonEncode(body));
    }

    final response = await request.close();
    final text = await utf8.decodeStream(response);
    final decoded = text.isEmpty ? <String, dynamic>{} : jsonDecode(text);
    if (decoded is! Map<String, dynamic>) {
      throw BackendApiException('Phản hồi máy chủ không hợp lệ', statusCode: response.statusCode);
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message = decoded['message'] is String ? decoded['message'] as String : 'Lỗi máy chủ';
      throw BackendApiException(message, statusCode: response.statusCode);
    }
    final apiResponse = ApiResponse<Object?>.fromJson(decoded, (json) => json);
    if (!apiResponse.success) {
      throw BackendApiException(apiResponse.message, statusCode: response.statusCode);
    }
    return apiResponse.data;
  }
}
