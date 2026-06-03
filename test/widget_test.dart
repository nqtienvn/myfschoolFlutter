// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/packages/core/core.dart';


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