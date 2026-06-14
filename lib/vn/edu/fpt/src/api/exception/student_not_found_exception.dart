class StudentNotFoundException implements Exception {
  final int studentId;

  const StudentNotFoundException(this.studentId);

  @override
  String toString() => 'Student id=$studentId was not found.';
}
