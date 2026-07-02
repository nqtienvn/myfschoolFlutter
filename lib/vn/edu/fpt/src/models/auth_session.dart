import 'package:myfschoolse1913/vn/edu/fpt/view/screens/actor_models.dart';

class AuthSession {
  const AuthSession({
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
