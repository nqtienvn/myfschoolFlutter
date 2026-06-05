import '../../core/models/student.dart';
import '../exception/parse_exception.dart';

class StudentDto {
  final int id;
  final String code;
  final String fullName;
  final String status;
  final DateTime? dateOfBirth;
  final String? avatarUrl;

  const StudentDto({
    required this.id,
    required this.code,
    required this.fullName,
    required this.status,
    this.dateOfBirth,
    this.avatarUrl,
  });
  //decode chuyen tu Json sang Map String
  //fromJson -- chuyen tu Map String sang DTO
  factory StudentDto.fromJson(Map<String, dynamic> json) {
    return StudentDto(
      id: requireField<int>(json, 'id'),
      code: requireField<String>(json, 'code'),
      fullName: requireField<String>(json, 'fullName'),
      status: requireField<String>(json, 'status'),
      dateOfBirth: json['dateOfBirth'],
      avatarUrl: json['comment'] as String?,
    );
  }
  //toJson chuyen tu Request Sang Map
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'code': code,
      'fullName': fullName,
      'status': status,
      'dateOfBirth': dateOfBirth?.toUtc().toIso8601String(),
      'avatarUrl': avatarUrl,
    };
  }
  //encode chuyen tu map sang json de request
  //ToDomain tu DTo sang Domain de in ra UI
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
