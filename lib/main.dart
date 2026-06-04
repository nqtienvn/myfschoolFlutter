import 'package:myfschoolse1913/vn/edu/fpt/packages/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/packages/api/dto/studentdemo_dto.dart';
import 'package:myfschoolse1913/vn/edu/fpt/packages/core/core.dart';
import 'dart:convert';

void main() {
  const rawJson = '''
  {
    "data": {
      "studentId": 10,
      "items": [
        {
          "id": 101,
          "subjectName": "Toán",
          "value": 8.5,
          "weight": 1,
          "createdAt": "2026-06-03T08:30:00Z",
          "comment": null
        },
        {
          "id": 102,
          "subjectName": "Văn",
          "value": 9,
          "weight": 2,
          "createdAt": "2026-06-04T08:30:00Z",
          "comment": "Bài viết tốt"
        }
      ]
    },
    "meta": null,
    "error": null
  }
  ''';

  final root = jsonDecode(rawJson) as Map<String, dynamic>;
  final data = root['data'] as Map<String, dynamic>;
  final items = data['items'] as List<dynamic>;

  final List<Grade> grades = items
      .map((item) => GradeDto.fromJson(
    item as Map<String, dynamic>,
  ).toDomain())
      .toList();
  final studentDto = StudentDemoDto.fromJson(data);
  print('${studentDto.id}: ${studentDto.name}');
  for (final grade in grades) {
    print(
      '${grade.subjectName}: ${grade.value} - ${grade.displayComment}',
    );
  }
  print('Điểm trung bình: ${gradeAverage(grades).toStringAsFixed(2)}');
}
