import 'package:myfschoolse1913/vn/edu/fpt/src/core/core.dart';


class StudentReportFormatter {
  const StudentReportFormatter();
  String format(StudentSummary summary) {
    final buffer = StringBuffer();
    final attendanceRate =
    (summary.attendance.attendanceRate * 100).toStringAsFixed(0);

    buffer.writeln('=' * 50);
    buffer.writeln('EDULINK - BÁO CÁO HỌC SINH');
    buffer.writeln('=' * 50);
    buffer.writeln(
      'Học sinh           : '
          '${summary.student.fullName} (${summary.student.code})',
    );
    buffer.writeln(
      'Trạng thái         : ${summary.student.status}',
    );
    buffer.writeln(
      'Điểm trung bình    : '
          '${summary.averageGrade.toStringAsFixed(2)}',
    );
    buffer.writeln(
      'Điểm TB trọng số   : '
          '${summary.weightedAverageGrade.toStringAsFixed(2)}',
    );
    buffer.writeln(
      'Số buổi học        : ${summary.attendance.totalSessions}',
    );
    buffer.writeln(
      'Tỷ lệ chuyên cần   : $attendanceRate%',
    );
    buffer.writeln(
      'Số buổi vắng       : ${summary.attendance.absentCount}',
    );
    buffer.writeln(
      'Bài tập chưa nộp   : ${summary.missingHomeworkCount}',
    );
    buffer.writeln(
      'Thông báo chưa đọc : ${summary.unreadAnnouncementCount}',
    );
    buffer.writeln(
      'Đánh giá           : ${summary.shortStatus}',
    );
    buffer.writeln('=' * 50);

    return buffer.toString();
  }

}