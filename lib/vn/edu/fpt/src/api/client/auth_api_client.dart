import '../dto/auth_session_dto.dart';
import '../exception/parse_exception.dart';
import 'backend_api_client.dart';

class AuthApiClient {
  const AuthApiClient({required BackendApiClient backend}) : _backend = backend;

  final BackendApiClient _backend;

  Future<AuthSessionDto> login({
    required String phone,
    required String password,
  }) async {
    final data = await _backend.postData(
      '/api/auth/login',
      body: {'phone': phone, 'password': password},
    );
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Login response data must be object.');
    }
    return AuthSessionDto.fromJson(data);
  }

  Future<void> changePassword({
    required String token,
    required String oldPassword,
    required String newPassword,
  }) async {
    await _backend.putData(
      '/api/user/password',
      token: token,
      body: {'oldPassword': oldPassword, 'newPassword': newPassword},
    );
  }

  Future<void> requestPasswordReset({required String phone}) async {
    await _backend.postData(
      '/api/auth/password-reset/request',
      body: {'phone': phone},
    );
  }
}
