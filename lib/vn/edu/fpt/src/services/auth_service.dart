import '../api/client/auth_api_client.dart';
import '../models/auth_session.dart';

class AuthService {
  AuthService({required AuthApiClient apiClient}) : _apiClient = apiClient;

  final AuthApiClient _apiClient;
  AuthSession? _currentSession;

  AuthSession? get currentSession => _currentSession;
  bool get isLoggedIn => _currentSession != null;

  Future<AuthSession> login(String phone, String password) async {
    final dto = await _apiClient.login(phone: phone, password: password);
    final session = dto.toDomain();
    _currentSession = session;
    return session;
  }

  void logout() {
    _currentSession = null;
  }
}
