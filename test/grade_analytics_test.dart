import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/models.dart';

void main() {
  group('grade analytics', () {
    test('gradeAverage returns 0 for empty list', () {
      expect(gradeAverage([]), 0);
    });

    test('gradeAverage calculates simple average', () {
      final grades = [
        Grade(
          id: 1,
          subjectName: 'Toán',
          value: 8,
          weight: 1,
          createdAt: DateTime(2026, 6, 1),
        ),
        Grade(
          id: 2,
          subjectName: 'Toán',
          value: 10,
          weight: 1,
          createdAt: DateTime(2026, 6, 2),
        ),
      ];

      expect(gradeAverage(grades), 9);
    });

    test('weightedGradeAverage respects weight', () {
      final grades = [
        Grade(
          id: 1,
          subjectName: 'Toán',
          value: 8,
          weight: 1,
          createdAt: DateTime(2026, 6, 1),
        ),
        Grade(
          id: 2,
          subjectName: 'Toán',
          value: 10,
          weight: 2,
          createdAt: DateTime(2026, 6, 2),
        ),
      ];

      expect(weightedGradeAverage(grades), closeTo(9.333, 0.001));
    });
  });
}
