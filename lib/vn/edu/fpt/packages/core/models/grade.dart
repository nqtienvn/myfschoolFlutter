class Grade {
  final int id;
  final String subjectName;
  final double value;
  final double weight; //he so cua diem
  final DateTime createdAt;
  final String? comment;

  const Grade({
    required this.id,
    required this.subjectName,
    required this.value,
    required this.weight,
    required this.createdAt,
    this.comment,
  });

  String get displayComment {
    return comment ?? 'Không có nhận xét';
  }

  bool get isPassed {
    return value >= 5.0;
  }
}
