import '../api/client/auth_api_client.dart';
import '../models/auth_session.dart';
import 'package:flutter/foundation.dart';

class AuthService extends ChangeNotifier {
  AuthService({required AuthApiClient apiClient}) : _apiClient = apiClient;

  final AuthApiClient _apiClient;
  AuthSession? _currentSession;
  int _selectedChildIndex = 0;

  AuthSession? get currentSession => _currentSession;
  bool get isLoggedIn => _currentSession != null;
  int get selectedChildIndex => _selectedChildIndex;
  LinkedStudent? get selectedChild {
    final children = _currentSession?.children ?? const [];
    if (children.isEmpty) return null;
    final index = _selectedChildIndex.clamp(0, children.length - 1);
    return children[index];
  }

  Future<AuthSession> login(String phone, String password) async {
    final dto = await _apiClient.login(phone: phone, password: password);
    final session = dto.toDomain();
    _currentSession = session;
    _selectedChildIndex = 0;
    notifyListeners();
    return session;
  }

  Future<void> requestPasswordReset(String phone) {
    return _apiClient.requestPasswordReset(phone: phone);
  }

  void selectChild(int index) {
    final children = _currentSession?.children ?? const [];
    if (index < 0 || index >= children.length || index == _selectedChildIndex) return;
    _selectedChildIndex = index;
    notifyListeners();
  }

  void logout() {
    _currentSession = null;
    _selectedChildIndex = 0;
    notifyListeners();
  }
}
