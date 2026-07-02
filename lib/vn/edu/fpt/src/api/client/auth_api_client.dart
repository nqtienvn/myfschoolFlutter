import '../dto/auth_session_dto.dart';
import '../exception/parse_exception.dart';
import 'backend_api_client.dart';

class AuthApiClient {
  const AuthApiClient({required BackendApiClient backend}) : _backend = backend;

  final BackendApiClient _backend;

  Future<AuthSessionDto> login({required String phone, required String password}) async {
    final data = await _backend.postData(
      '/api/auth/login',
      body: {'phone': phone, 'password': password},
    );
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Login response data must be object.');
    }
    return AuthSessionDto.fromJson(data);
  }
}
