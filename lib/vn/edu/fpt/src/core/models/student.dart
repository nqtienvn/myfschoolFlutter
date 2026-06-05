class Student {
  final int id;
  final String code;
  final String fullName;
  final String status;
  final DateTime? dateOfBirth;
  final String? avatarUrl;

  const Student({
    required this.id,
    required this.code,
    required this.fullName,
    required this.status,
    this.dateOfBirth,
    this.avatarUrl,
  });

  String get displayCode {
    return '$code - $fullName';
  }
}
