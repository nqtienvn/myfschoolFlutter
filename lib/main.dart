import 'package:myfschoolse1913/vn/edu/fpt/packages/core/core.dart';
  // const user = User(
  //   id: 1,
  //   email: 'parent@edulink.test',
  //   fullName: 'Nguyễn Văn Bình',
  //   role: UserRole.guardian,
  //   active: true,
  //   phone: null,
  // );
  //
  // final student = Student(
  //   id: 10,
  //   code: 'STU-001',
  //   fullName: 'Nguyễn Minh An',
  //   status: 'ACTIVE',
  //   dateOfBirth: DateTime(2010, 9, 1),
  // );
  //
  // final grade = Grade(
  //   id: 100,
  //   subjectName: 'Toán',
  //   value: 8.5,
  //   weight: 1.0,
  //   createdAt: DateTime.now(),
  //   comment: null,
  // );
  //
  //   print(user.displayName);
  //   print(student.displayCode);
  //   print('${grade.subjectName}: ${grade.value} - ${grade.displayComment}');
  // void main() {
  //   final grades = [
  //     Grade(
  //       id: 1,
  //       subjectName: 'Toán',
  //       value: 8.5,
  //       weight: 1,
  //       createdAt: DateTime(2026, 6, 1),
  //     ),
  //     Grade(
  //       id: 2,
  //       subjectName: 'Toán',
  //       value: 7.5,
  //       weight: 2,
  //       createdAt: DateTime(2026, 6, 2),
  //       comment: 'Cần cẩn thận hơn ở bài hình.',
  //     ),
  //     Grade(
  //       id: 3,
  //       subjectName: 'Văn',
  //       value: 9.0,
  //       weight: 1,
  //       createdAt: DateTime(2026, 6, 3),
  //     ),
  //   ];
  //
  //   final mathGrades = gradesBySubject(
  //     grades: grades,
  //     subjectName: 'Toán',
  //   );
  //
  //   print('Các môn: ${subjectNames(grades).join(', ')}');
  //   print('Điểm TB Toán: ${gradeAverage(mathGrades).toStringAsFixed(2)}');
  //   print('Điểm TB Toán có trọng số: ${weightedGradeAverage(mathGrades).toStringAsFixed(2)}');
  // }

void main() {
  final student = Student(
    id: 10,
    code: 'STU-001',
    fullName: 'Nguyễn Minh An',
    status: 'ACTIVE',
    dateOfBirth: DateTime(2010, 9, 1),
  );

  final grades = [
    Grade(
      id: 1,
      subjectName: 'Toán',
      value: 8.5,
      weight: 1,
      createdAt: DateTime(2026, 6, 1),
    ),
    Grade(
      id: 2,
      subjectName: 'Văn',
      value: 7.5,
      weight: 1,
      createdAt: DateTime(2026, 6, 2),
    ),
  ];

  final attendance = AttendanceStats.fromStatuses([
    'PRESENT',
    'PRESENT',
    'LATE',
    'ABSENT',
  ]);

  final summary = StudentSummary(
    student: student,
    grades: grades,
    attendance: attendance,
    unreadAnnouncementCount: 2,
    missingHomeworkCount: 1,
  );

  print('Học sinh: ${summary.student.fullName}');
  print('Điểm trung bình: ${summary.averageGrade.toStringAsFixed(1)}');
  print('Tỷ lệ chuyên cần: ${(summary.attendance.attendanceRate * 100).toStringAsFixed(0)}%');
  print('Trạng thái: ${summary.shortStatus}');
}

