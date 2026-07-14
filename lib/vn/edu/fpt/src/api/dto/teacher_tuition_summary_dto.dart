import '../exception/parse_exception.dart';

class TeacherTuitionStudentDto {
  const TeacherTuitionStudentDto({
    required this.studentId,
    required this.studentName,
    required this.studentCode,
    required this.paymentState,
    required this.outstandingAmount,
  });

  final int studentId;
  final String studentName;
  final String studentCode;
  final String paymentState;
  final double outstandingAmount;

  bool get isPaid => paymentState == 'PAID';
  bool get hasOutstanding => !isPaid && paymentState != 'NO_BILLS';

  factory TeacherTuitionStudentDto.fromJson(Map<String, dynamic> json) {
    final paymentState = _requiredString(json, 'paymentState');
    if (!const {
      'PAID',
      'UNPAID',
      'PROCESSING',
      'NO_BILLS',
    }.contains(paymentState)) {
      throw const ParseException('Trạng thái học phí học sinh không hợp lệ.');
    }
    final outstandingAmount = _requiredDouble(json, 'outstandingAmount');
    if (outstandingAmount < 0) {
      throw const ParseException('Số tiền học phí còn lại không hợp lệ.');
    }
    return TeacherTuitionStudentDto(
      studentId: _requiredInt(json, 'studentId'),
      studentName: _requiredString(json, 'studentName'),
      studentCode: _requiredString(json, 'studentCode'),
      paymentState: paymentState,
      outstandingAmount: outstandingAmount,
    );
  }
}

class TeacherTuitionSummaryDto {
  const TeacherTuitionSummaryDto({
    required this.classId,
    required this.className,
    required this.semesterId,
    required this.semesterName,
    required this.totalStudents,
    required this.paidStudents,
    required this.outstandingStudents,
    required this.studentsWithoutBills,
    required this.students,
  });

  final int classId;
  final String className;
  final int semesterId;
  final String semesterName;
  final int totalStudents;
  final int paidStudents;
  final int outstandingStudents;
  final int studentsWithoutBills;
  final List<TeacherTuitionStudentDto> students;

  factory TeacherTuitionSummaryDto.fromJson(Map<String, dynamic> json) {
    final rows = json['students'];
    if (rows is! List) {
      throw const ParseException('Dữ liệu tổng hợp học phí không hợp lệ.');
    }
    final students = rows
        .map((row) {
          if (row is! Map<String, dynamic>) {
            throw const ParseException(
              'Dữ liệu học phí học sinh không hợp lệ.',
            );
          }
          return TeacherTuitionStudentDto.fromJson(row);
        })
        .toList(growable: false);
    final totalStudents = _requiredInt(json, 'totalStudents');
    final paidStudents = _requiredInt(json, 'paidStudents');
    final outstandingStudents = _requiredInt(json, 'outstandingStudents');
    final studentsWithoutBills = _requiredInt(json, 'studentsWithoutBills');
    final derivedPaid = students.where((student) => student.isPaid).length;
    final derivedOutstanding = students
        .where((student) => student.hasOutstanding)
        .length;
    final derivedWithoutBills = students
        .where((student) => student.paymentState == 'NO_BILLS')
        .length;
    if (totalStudents != students.length ||
        paidStudents != derivedPaid ||
        outstandingStudents != derivedOutstanding ||
        studentsWithoutBills != derivedWithoutBills) {
      throw const ParseException(
        'Số liệu tổng hợp học phí không khớp danh sách học sinh.',
      );
    }
    return TeacherTuitionSummaryDto(
      classId: _requiredInt(json, 'classId'),
      className: _requiredString(json, 'className'),
      semesterId: _requiredInt(json, 'semesterId'),
      semesterName: _requiredString(json, 'semesterName'),
      totalStudents: totalStudents,
      paidStudents: paidStudents,
      outstandingStudents: outstandingStudents,
      studentsWithoutBills: studentsWithoutBills,
      students: students,
    );
  }
}

int _requiredInt(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! num || value.toInt() < 0) {
    throw ParseException('Dữ liệu học phí thiếu hoặc sai trường $key.');
  }
  return value.toInt();
}

double _requiredDouble(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! num) {
    throw ParseException('Dữ liệu học phí thiếu hoặc sai trường $key.');
  }
  return value.toDouble();
}

String _requiredString(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! String || value.trim().isEmpty) {
    throw ParseException('Dữ liệu học phí thiếu hoặc sai trường $key.');
  }
  return value;
}
