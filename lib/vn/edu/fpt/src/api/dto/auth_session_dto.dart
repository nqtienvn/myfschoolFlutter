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
    required this.phone,
    required this.status,
    this.email,
    this.accountCode,
    this.children = const [],
  });

  final String token;
  final String tokenType;
  final int expiresIn;
  final int userId;
  final String userName;
  final String role;
  final String phone;
  final String? email;
  final String status;
  final String? accountCode;
  final List<LinkedStudent> children;

  factory AuthSessionDto.fromJson(Map<String, dynamic> json) {
    final user = requireField<Map<String, dynamic>>(json, 'user');
    final role = requireField<String>(user, 'role');
    return AuthSessionDto(
      token: requireField<String>(json, 'token'),
      tokenType: json['tokenType'] is String
          ? json['tokenType'] as String
          : 'Bearer',
      expiresIn: requireField<int>(json, 'expiresIn'),
      userId: requireField<int>(user, 'id'),
      userName: requireField<String>(user, 'name'),
      role: role,
      phone: requireField<String>(user, 'phone'),
      email: user['email'] as String?,
      status: requireField<String>(user, 'status'),
      accountCode: _accountCode(user, role),
      children: _children(user, role),
    );
  }

  AuthSession toDomain() => AuthSession(
    token: token,
    tokenType: tokenType,
    expiresIn: expiresIn,
    userId: userId,
    userName: userName,
    role: role,
    phone: phone,
    email: email,
    status: status,
    accountCode: accountCode,
    children: children,
  );

  static List<LinkedStudent> _children(
    Map<String, dynamic> user,
    String role,
  ) {
    if (role.toUpperCase() != 'PARENT') return const [];
    final parentProfile = user['parentProfile'];
    if (parentProfile is! Map<String, dynamic>) return const [];
    final rawChildren = parentProfile['children'];
    if (rawChildren is! List) return const [];
    return rawChildren.whereType<Map<String, dynamic>>().map((child) {
      return LinkedStudent(
        id: requireField<int>(child, 'id'),
        name: requireField<String>(child, 'name'),
        studentCode: requireField<String>(child, 'studentCode'),
        status: child['status'] is String ? child['status'] as String : 'ACTIVE',
        className: child['className'] as String?,
        classId: child['classId'] as int?,
        schoolName: child['schoolName'] as String?,
        academicYearName: child['academicYearName'] as String?,
        dateOfBirth: child['dateOfBirth'] as String?,
        gender: child['gender'] as String?,
        address: child['address'] as String?,
        email: child['email'] as String?,
        avatar: child['avatar'] as String?,
      );
    }).toList(growable: false);
  }

  static String? _accountCode(Map<String, dynamic> user, String role) {
    switch (role.toUpperCase()) {
      case 'STUDENT':
        final profile = user['studentProfile'];
        return profile is Map<String, dynamic>
            ? profile['studentCode'] as String?
            : null;
      case 'TEACHER':
        final profile = user['teacherProfile'];
        return profile is Map<String, dynamic>
            ? profile['employeeCode'] as String?
            : null;
      case 'PARENT':
        final profile = user['parentProfile'];
        final id = profile is Map<String, dynamic> ? profile['id'] : null;
        return id is int ? 'PH-${id.toString().padLeft(6, '0')}' : null;
      default:
        return null;
    }
  }
}
