import 'package:myfschoolse1913/vn/edu/fpt/src/api/exception/parse_exception.dart';

import '../../models/user.dart';

class UserDto {
  final int id;
  final String email;
  final String fullName;
  final String role;
  final bool active;
  final String? phone;
  final String? avatarUrl;

  const UserDto({
    required this.id,
    required this.email,
    required this.fullName,
    required this.role,
    required this.active,
    this.phone,
    this.avatarUrl,
  });

  factory UserDto.fromJson(Map<String, dynamic> json) {
    return UserDto(
      id: requireField(json, 'id'),
      email: requireField(json, 'email'),
      fullName: requireField(json, 'fullName'),
      role: requireField(json, 'role'),
      active: requireField(json, 'active'),
      phone: json['phone'],
      avatarUrl: json['avatarUrl'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'fullName': fullName,
      'role': role,
      'active': active,
      'phone': phone,
      'avatarUrl': avatarUrl,
    };
  }

  User toDomain() {
    return User(
      id: id,
      email: email,
      fullName: fullName,
      role: parseRole(role),
      active: active,
      phone: phone,
      avatarUrl: avatarUrl,
    );
  }

  UserRole parseRole(String role) {
    switch (role.toUpperCase()) {
      case 'GUARDIAN':
        return UserRole.guardian;
      case 'STUDENT':
        return UserRole.student;
      case 'TEACHER':
        return UserRole.teacher;
      case 'ADMIN':
        return UserRole.admin;
      default:
        throw ParseException('Invalid role: $role');
    }
  }
}
