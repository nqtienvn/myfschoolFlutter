import 'package:myfschoolse1913/vn/edu/fpt/src/core/core.dart';

class StudentDto {
  final int id;
  final String code;
  final String fullName;
  final String status;
  final DateTime? dateOfBirth;
  final String? avatarUrl;

  StudentDto({
    required this.id,
    required this.code,
    required this.fullName,
    required this.status,
    this.dateOfBirth,
    this.avatarUrl,
  });

  factory StudentDto.fromJson(Map<String, dynamic> json) {
    final rawDateOfBirth = json['dateOfBirth'];
    return StudentDto(
      id: json['id'] as int,
      code: json['code'] as String,
      fullName: json['fullName'] as String,
      status: json['status'] as String,
      dateOfBirth: rawDateOfBirth == null
          ? null
          : DateTime.parse(rawDateOfBirth as String),
      avatarUrl: json['avatarUrl'] as String?,
    );
  }

  Student toDomain() {
    return Student(
      id: id,
      code: code,
      fullName: fullName,
      status: status,
      dateOfBirth: dateOfBirth,
      avatarUrl: avatarUrl,
    );
  }
}
