import '../models/grade.dart';

enum AcademicTrend { improving, declining, stable, unknown }

AcademicTrend academicTrend({
  required List<Grade> oldGrades,
  required List<Grade> newGrades,
}) {
  if (oldGrades.isEmpty || newGrades.isEmpty) {
    return AcademicTrend.unknown;
  }
  final oldAvg = weightedGradeAverage(oldGrades);
  final newAvg = weightedGradeAverage(newGrades);
  final diff = newAvg - oldAvg;
  if (diff >= 0.5) {
    return AcademicTrend.improving;
  } else if (diff <= -0.5) {
    return AcademicTrend.declining;
  }
  return AcademicTrend.unknown;
}

double gradeAverage(List<Grade> grades) {
  if (grades.isEmpty) return 0;
  final total = grades.fold<double>(0, (sum, grade) => sum + grade.value);
  return total / grades.length;
}

double weightedGradeAverage(List<Grade> grades) {
  if (grades.isEmpty) return 0;
  final totalWeight = grades.fold<double>(
    0,
    (sum, grade) => sum + grade.weight,
  );
  if (totalWeight == 0) return 0;
  final weightedTotal = grades.fold<double>(
    0,
    (sum, grade) => sum + grade.value * grade.weight,
  );
  return weightedTotal / totalWeight;
}

List<Grade> gradesBySubject({
  required List<Grade> grades,
  required String subjectName,
}) {
  return grades.where((grade) => grade.subjectName == subjectName).toList();
}

Set<String> subjectNames(List<Grade> grades) {
  return grades.map((grade) => grade.subjectName).toSet();
}
