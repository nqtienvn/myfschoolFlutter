import 'package:myfschoolse1913/vn/edu/fpt/src/api/exception/parse_exception.dart';

import '../../models/user.dart';

class UserDto {
  final int id;
  final String email;
  final String fullName;
  final String role;
  final bool active;
  final String? phone;

  const UserDto({
    required this.id,
    required this.email,
    required this.fullName,
    required this.role,
    required this.active,
    this.phone,
  });

  factory UserDto.fromJson(Map<String, dynamic> json) {
    return UserDto(
      id: requireField(json, 'id'),
      email: requireField(json, 'email'),
      fullName: requireField(json, 'fullName'),
      role: requireField(json, 'role'),
      active: requireField(json, 'active'),
      phone: json['phone'],
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
