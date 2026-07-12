enum GradeEntryRole { subjectTeacher, admin, both }

class GradeColumn {
  final int id;
  final String code;
  final String name;
  final int weight;
  final GradeEntryRole entryRole;
  const GradeColumn({
    required this.id,
    required this.code,
    required this.name,
    required this.weight,
    required this.entryRole,
  });
  bool get teacherCanEdit => entryRole != GradeEntryRole.admin;
}

class SubjectTranscript {
  final String subjectName;
  final List<GradeColumn> columns;
  final Map<int, double?> scores;
  final double? average;
  const SubjectTranscript({
    required this.subjectName,
    required this.columns,
    required this.scores,
    this.average,
  });
  bool get complete => columns.every((column) => scores[column.id] != null);
}
