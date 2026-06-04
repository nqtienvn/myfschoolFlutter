import 'package:myfschoolse1913/vn/edu/fpt/packages/api/exception/parse_exception.dart';

class StudentDemoDto {
  final int id;
  final String name;

  const StudentDemoDto({required this.id, required this.name});

  factory StudentDemoDto.fromJson(Map<String, dynamic> json) {
    return StudentDemoDto(
      id: requireField<int>(json, 'studentId'),
      name: 'Nguyễn Quang Tiền',
    );
  }
}
