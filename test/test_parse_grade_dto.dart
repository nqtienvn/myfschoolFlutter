import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/dto/grade_dto.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/exception/parse_exception.dart';

void main() {
  group('GradeDto.fromJson', () {
    test('parses int and double numbers', () {
      final dto = GradeDto.fromJson({
        'id': 101,
        'subjectName': 'Toán',
        'value': 8.5,
        'weight': 1,
        'createdAt': '2026-06-03T08:30:00Z',
        'comment': null,
      });

      expect(dto.value, 8.5);
      expect(dto.weight, 1.0);
      expect(dto.comment, isNull);
    });

    test('maps DTO to domain', () {
      final dto = GradeDto.fromJson({
        'id': 101,
        'subjectName': 'Toán',
        'value': 8,
        'weight': 1,
        'createdAt': '2026-06-03T08:30:00Z',
        'comment': 'Tốt',
      });

      final grade = dto.toDomain();

      expect(grade.subjectName, 'Toán');
      expect(grade.displayComment, 'Tốt');
    });

    test('throws when value is not a number', () {
      expect(
            () => GradeDto.fromJson({
          'id': 101,
          'subjectName': 'Toán',
          'value': 'k',
          'weight': 1,
          'createdAt': '2026-06-03T08:30:00Z',
          'comment': null,
        }),
        throwsA(isA<ParseException>()),
      );
    });
  });
}
