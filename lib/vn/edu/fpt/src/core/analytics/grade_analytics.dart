import '../models/grade.dart';

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
