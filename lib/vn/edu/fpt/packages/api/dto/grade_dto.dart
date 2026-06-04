import 'package:myfschoolse1913/vn/edu/fpt/packages/core/models/grade.dart';

import '../exception/parse_exception.dart';

class GradeDto {
  final int id;
  final String subjectName;
  final double value;
  final double weight;
  final DateTime createdAt;
  final String? comment;

  const GradeDto({
    required this.id,
    required this.subjectName,
    required this.value,
    required this.weight,
    required this.createdAt,
    this.comment,
  });
  factory GradeDto.fromJson(Map<String, dynamic> json) { //tra ve json lay tu backend(can co jsondecode de lay ve map da)
    final rawValue = json['value'];
    final rawWeight = json['weight'];
    final rawCreatedAt = json['createdAt'];

    if (rawValue is! num) {
      throw const ParseException('Field "value" must is number.');
    }

    if (rawWeight is! num) {
      throw const ParseException('Field "weight" must is number.');
    }

    if (rawCreatedAt is! String) {
      throw const ParseException('Field "createdAt" must is String.');
    }

    final createdAt = DateTime.tryParse(rawCreatedAt);
    if (createdAt == null) {
      throw ParseException(
        'Field "createdAt" must not ISO-8601 hợp lệ: $rawCreatedAt',
      );
    }

    return GradeDto(
      id: requireField<int>(json, 'id'),
      subjectName: requireField<String>(json, 'subjectName'),
      value: rawValue.toDouble(),
      weight: rawWeight.toDouble(),
      createdAt: createdAt,
      comment: json['comment'] as String?,
    );

  }
  Map<String, dynamic> toJson() { //chuyen object dart thanh son de chuyen len backend
    return {
      'id': id,
      'subjectName': subjectName,
      'value': value,
      'weight': weight,
      'createdAt': createdAt.toUtc().toIso8601String(),
      'comment': comment,
    };
  }
  Grade toDomain() { // tra ve domain de hien thi len widget
    return Grade(
      id: id,
      subjectName: subjectName,
      value: value,
      weight: weight,
      createdAt: createdAt,
      comment: comment,
    );
  }

}