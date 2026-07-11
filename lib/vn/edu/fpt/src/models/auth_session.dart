import 'package:myfschoolse1913/vn/edu/fpt/view/screens/actor_models.dart';

class AuthSession {
  const AuthSession({
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

  bool get isActive => status.toUpperCase() == 'ACTIVE';

  AppActor get actor {
    switch (role.toUpperCase()) {
      case 'TEACHER':
        return AppActor.teacher;
      case 'STUDENT':
        return AppActor.student;
      case 'PARENT':
      default:
        return AppActor.parent;
    }
  }
}

class LinkedStudent {
  const LinkedStudent({
    required this.id,
    required this.name,
    required this.studentCode,
    required this.status,
    this.className,
    this.classId,
    this.schoolName,
    this.academicYearName,
    this.dateOfBirth,
    this.gender,
    this.address,
    this.email,
    this.avatar,
  });

  final int id;
  final String name;
  final String studentCode;
  final String status;
  final String? className;
  final int? classId;
  final String? schoolName;
  final String? academicYearName;
  final String? dateOfBirth;
  final String? gender;
  final String? address;
  final String? email;
  final String? avatar;
}
