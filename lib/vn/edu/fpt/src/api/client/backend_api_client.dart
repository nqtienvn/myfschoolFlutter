import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../dto/api_response.dart';
import '../exception/backend_api_exception.dart';

class BackendApiClient {
  BackendApiClient({String? baseUrl})
    : baseUri = Uri.parse(baseUrl ?? _defaultBaseUrl);

  static const _configuredBaseUrl = String.fromEnvironment('API_BASE_URL');

  static String get _defaultBaseUrl {
    if (_configuredBaseUrl.isNotEmpty) {
      return _configuredBaseUrl;
    }

    // 10.0.2.2 is Android Emulator's alias for the development machine.
    if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:8080';
    }

    return 'http://localhost:8080';
  }

  final Uri baseUri;
  final http.Client _client = http.Client();

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

  Future<Object?> postDataWithQuery(
    String path, {
    String? token,
    Map<String, String?> query = const {},
    Object? body,
  }) {
    return _send('POST', path, token: token, query: query, body: body);
  }

  Future<Object?> putData(String path, {String? token, Object? body}) {
    return _send('PUT', path, token: token, body: body);
  }

  Future<Object?> deleteData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
    Object? body,
  }) {
    return _send('DELETE', path, token: token, query: query, body: body);
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
    final request = http.Request(method, uri);
    request.headers['accept'] = 'application/json';
    request.headers['content-type'] = 'application/json; charset=utf-8';
    if (token != null) {
      request.headers['authorization'] = 'Bearer $token';
    }
    if (body != null) {
      request.body = jsonEncode(body);
    }

    final streamed = await _client.send(request);
    final text = await streamed.stream.bytesToString();
    final decoded = text.isEmpty ? <String, dynamic>{} : jsonDecode(text);
    if (decoded is! Map<String, dynamic>) {
      throw BackendApiException(
        'Phản hồi máy chủ không hợp lệ',
        statusCode: streamed.statusCode,
      );
    }
    if (streamed.statusCode < 200 || streamed.statusCode >= 300) {
      final message = decoded['message'] is String
          ? decoded['message'] as String
          : 'Lỗi máy chủ';
      throw BackendApiException(message, statusCode: streamed.statusCode);
    }
    final apiResponse = ApiResponse<Object?>.fromJson(decoded, (json) => json);
    if (!apiResponse.success) {
      throw BackendApiException(
        apiResponse.message,
        statusCode: streamed.statusCode,
      );
    }
    return apiResponse.data;
  }
}
