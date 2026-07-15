import '../exception/parse_exception.dart';

class HomeroomClassDetailDto {
  const HomeroomClassDetailDto({
    required this.id,
    required this.name,
    required this.gradeLevel,
    required this.academicYearId,
    required this.academicYearName,
    required this.schoolName,
    required this.students,
  });

  final int id;
  final String name;
  final int gradeLevel;
  final int academicYearId;
  final String academicYearName;
  final String schoolName;
  final List<HomeroomStudentDto> students;

  factory HomeroomClassDetailDto.fromJson(Map<String, dynamic> json) {
    final rawStudents = json['students'];
    if (rawStudents is! List) {
      throw const ParseException(
        'Danh sách học sinh trong hồ sơ lớp không hợp lệ.',
      );
    }
    return HomeroomClassDetailDto(
      id: _requiredInt(json, 'id', 'hồ sơ lớp'),
      name: _requiredString(json, 'name', 'hồ sơ lớp'),
      gradeLevel: _requiredInt(json, 'gradeLevel', 'hồ sơ lớp'),
      academicYearId: _requiredInt(json, 'academicYearId', 'hồ sơ lớp'),
      academicYearName: _requiredString(json, 'academicYearName', 'hồ sơ lớp'),
      schoolName: json['schoolName'] as String? ?? '',
      students: rawStudents
          .map((item) {
            if (item is! Map<String, dynamic>) {
              throw const ParseException('Thông tin học sinh không hợp lệ.');
            }
            return HomeroomStudentDto.fromJson(item);
          })
          .toList(growable: false),
    );
  }
}

class HomeroomStudentDto {
  const HomeroomStudentDto({
    required this.id,
    required this.name,
    required this.studentCode,
    required this.className,
  });

  final int id;
  final String name;
  final String studentCode;
  final String className;

  factory HomeroomStudentDto.fromJson(Map<String, dynamic> json) {
    return HomeroomStudentDto(
      id: _requiredInt(json, 'id', 'học sinh'),
      name: _requiredString(json, 'name', 'học sinh'),
      studentCode: _requiredString(json, 'studentCode', 'học sinh'),
      className: json['className'] as String? ?? '',
    );
  }
}

class HomeroomClassRankingDto {
  const HomeroomClassRankingDto({
    required this.classId,
    required this.className,
    required this.semesterId,
    required this.semesterName,
    required this.rankings,
  });

  final int classId;
  final String className;
  final int semesterId;
  final String semesterName;
  final List<HomeroomRankEntryDto> rankings;

  factory HomeroomClassRankingDto.fromJson(Map<String, dynamic> json) {
    final rawRankings = json['rankings'];
    if (rawRankings is! List) {
      throw const ParseException('Dữ liệu xếp hạng lớp không hợp lệ.');
    }
    return HomeroomClassRankingDto(
      classId: _requiredInt(json, 'classId', 'xếp hạng lớp'),
      className: _requiredString(json, 'className', 'xếp hạng lớp'),
      semesterId: _requiredInt(json, 'semesterId', 'xếp hạng lớp'),
      semesterName: _requiredString(json, 'semesterName', 'xếp hạng lớp'),
      rankings: rawRankings
          .map((item) {
            if (item is! Map<String, dynamic>) {
              throw const ParseException('Một dòng xếp hạng không hợp lệ.');
            }
            return HomeroomRankEntryDto.fromJson(item);
          })
          .toList(growable: false),
    );
  }
}

class HomeroomRankEntryDto {
  const HomeroomRankEntryDto({
    required this.rank,
    required this.studentId,
    required this.studentName,
    required this.studentCode,
    required this.gpa,
    required this.academicAbility,
    required this.conduct,
  });

  final int rank;
  final int studentId;
  final String studentName;
  final String studentCode;
  final double? gpa;
  final String? academicAbility;
  final String? conduct;

  factory HomeroomRankEntryDto.fromJson(Map<String, dynamic> json) {
    return HomeroomRankEntryDto(
      rank: _requiredInt(json, 'rank', 'xếp hạng học sinh'),
      studentId: _requiredInt(json, 'studentId', 'xếp hạng học sinh'),
      studentName: _requiredString(json, 'studentName', 'xếp hạng học sinh'),
      studentCode: _requiredString(json, 'studentCode', 'xếp hạng học sinh'),
      gpa: _nullableDouble(json['gpa'], 'gpa'),
      academicAbility: json['academicAbility'] as String?,
      conduct: json['conduct'] as String?,
    );
  }
}

class HomeroomStudentResultDto {
  const HomeroomStudentResultDto({
    required this.id,
    required this.studentId,
    required this.studentName,
    required this.semesterId,
    required this.semesterName,
    required this.classId,
    required this.className,
    required this.gpa,
    required this.rank,
    required this.honor,
    required this.conduct,
    required this.academicAbility,
  });

  final int id;
  final int studentId;
  final String studentName;
  final int semesterId;
  final String semesterName;
  final int classId;
  final String className;
  final double? gpa;
  final int? rank;
  final String? honor;
  final String? conduct;
  final String? academicAbility;

  factory HomeroomStudentResultDto.fromJson(Map<String, dynamic> json) {
    return HomeroomStudentResultDto(
      id: _requiredInt(json, 'id', 'kết quả học kỳ'),
      studentId: _requiredInt(json, 'studentId', 'kết quả học kỳ'),
      studentName: _requiredString(json, 'studentName', 'kết quả học kỳ'),
      semesterId: _requiredInt(json, 'semesterId', 'kết quả học kỳ'),
      semesterName: _requiredString(json, 'semesterName', 'kết quả học kỳ'),
      classId: _requiredInt(json, 'classId', 'kết quả học kỳ'),
      className: _requiredString(json, 'className', 'kết quả học kỳ'),
      gpa: _nullableDouble(json['gpa'], 'gpa'),
      rank: _nullableInt(json['rank'], 'rank'),
      honor: json['honor'] as String?,
      conduct: json['conduct'] as String?,
      academicAbility: json['academicAbility'] as String?,
    );
  }
}

int _requiredInt(Map<String, dynamic> json, String key, String context) {
  final value = json[key];
  if (value is! num) {
    throw ParseException('Trường $key của $context không hợp lệ.');
  }
  return value.toInt();
}

int? _nullableInt(Object? value, String field) {
  if (value == null) return null;
  if (value is! num) throw ParseException('Trường $field không hợp lệ.');
  return value.toInt();
}

double? _nullableDouble(Object? value, String field) {
  if (value == null) return null;
  if (value is! num) throw ParseException('Trường $field không hợp lệ.');
  return value.toDouble();
}

String _requiredString(Map<String, dynamic> json, String key, String context) {
  final value = json[key];
  if (value is! String || value.trim().isEmpty) {
    throw ParseException('Trường $key của $context không hợp lệ.');
  }
  return value.trim();
}
