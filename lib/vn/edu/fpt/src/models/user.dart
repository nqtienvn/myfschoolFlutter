enum UserRole { guardian, student, teacher, admin }

class User {
  final int id;
  final String email;
  final String fullName;
  final UserRole role;
  final bool active;
  final String? phone;
  final String? avatarUrl;

  const User({
    required this.id,
    required this.email,
    required this.fullName,
    required this.role,
    required this.active,
    this.phone,
    this.avatarUrl,
  });

  String get displayName {
    String userName = fullName.trim().isEmpty ? email : fullName;
    return userName;
  }
}
