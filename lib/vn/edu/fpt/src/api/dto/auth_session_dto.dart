import '../../models/auth_session.dart';
import '../exception/parse_exception.dart';

class AuthSessionDto {
  const AuthSessionDto({
    required this.token,
    required this.tokenType,
    required this.expiresIn,
    required this.userId,
    required this.userName,
    required this.role,
  });

  final String token;
  final String tokenType;
  final int expiresIn;
  final int userId;
  final String userName;
  final String role;

  factory AuthSessionDto.fromJson(Map<String, dynamic> json) {
    final user = requireField<Map<String, dynamic>>(json, 'user');
    return AuthSessionDto(
      token: requireField<String>(json, 'token'),
      tokenType: json['tokenType'] is String ? json['tokenType'] as String : 'Bearer',
      expiresIn: requireField<int>(json, 'expiresIn'),
      userId: requireField<int>(user, 'id'),
      userName: requireField<String>(user, 'name'),
      role: requireField<String>(user, 'role'),
    );
  }

  AuthSession toDomain() => AuthSession(
        token: token,
        tokenType: tokenType,
        expiresIn: expiresIn,
        userId: userId,
        userName: userName,
        role: role,
      );
}
