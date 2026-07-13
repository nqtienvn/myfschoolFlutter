import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';

class StudentSnapshot {
  StudentSnapshot({
    this.id,
    required this.name,
    required this.shortName,
    required this.className,
    required this.school,
    required this.studentCode,
    required this.linkStatus,
    required this.homeroomTeacher,
    required this.homeroomPhone,
    required this.averageScore,
    required this.attendanceRate,
    required this.unreadNotifications,
    required this.pendingLeaveRequests,
    required this.subjects,
    required this.attendanceEvents,
    required this.scheduleItems,
    required this.leaveRequests,
    required this.notifications,
    required this.teacherMessages,
    required this.aiSummary,
    required this.tuitionBills,
    this.dateOfBirth,
    this.gender,
    this.address,
    this.email,
    this.academicYearName,
  });

  factory StudentSnapshot.linked({
    required int id,
    required String name,
    required String studentCode,
    required String className,
    required String school,
    required String linkStatus,
    String? dateOfBirth,
    String? gender,
    String? address,
    String? email,
    String? academicYearName,
  }) {
    final parts = name
        .trim()
        .split(RegExp(r'\s+'))
        .where((part) => part.isNotEmpty)
        .toList();
    final shortName = parts.isEmpty
        ? '--'
        : parts.length == 1
        ? parts.first.substring(0, 1).toUpperCase()
        : '${parts[parts.length - 2][0]}${parts.last[0]}'.toUpperCase();
    return StudentSnapshot(
      id: id,
      name: name,
      shortName: shortName,
      className: className,
      school: school,
      studentCode: studentCode,
      linkStatus: linkStatus,
      homeroomTeacher: 'Chưa cập nhật',
      homeroomPhone: 'Chưa cập nhật',
      averageScore: 0,
      attendanceRate: 0,
      unreadNotifications: 0,
      pendingLeaveRequests: 0,
      subjects: const [],
      attendanceEvents: const [],
      scheduleItems: const [],
      leaveRequests: const [],
      notifications: const [],
      teacherMessages: const [],
      aiSummary: '',
      tuitionBills: const [],
      dateOfBirth: dateOfBirth,
      gender: gender,
      address: address,
      email: email,
      academicYearName: academicYearName,
    );
  }

  final int? id;
  final String name;
  final String shortName;
  final String className;
  final String school;
  final String studentCode;
  final String linkStatus;
  final String homeroomTeacher;
  final String homeroomPhone;
  final double averageScore;
  final int attendanceRate;
  final int unreadNotifications;
  final int pendingLeaveRequests;
  final List<SubjectScore> subjects;
  final List<AttendanceEvent> attendanceEvents;
  final List<ScheduleItem> scheduleItems;
  final List<LeaveRequest> leaveRequests;
  final List<ParentNotification> notifications;
  final List<TeacherMessage> teacherMessages;
  final String aiSummary;
  final List<TuitionBill> tuitionBills;
  final String? dateOfBirth;
  final String? gender;
  final String? address;
  final String? email;
  final String? academicYearName;
}

class SubjectScore {
  const SubjectScore({
    required this.subject,
    required this.score,
    required this.comment,
    required this.trend,
    required this.color,
  });

  final String subject;
  final double score;
  final String comment;
  final double trend;
  final Color color;
}

class AttendanceEvent {
  const AttendanceEvent({
    required this.date,
    required this.session,
    required this.status,
    required this.reason,
    required this.color,
    required this.background,
  });

  final String date;
  final String session;
  final String status;
  final String reason;
  final Color color;
  final Color background;
}

class ScheduleItem {
  const ScheduleItem({
    required this.time,
    required this.title,
    required this.detail,
    required this.icon,
    required this.color,
  });

  final String time;
  final String title;
  final String detail;
  final IconData icon;
  final Color color;
}

class LeaveRequest {
  const LeaveRequest({
    this.id,
    this.studentId,
    required this.title,
    required this.date,
    required this.reason,
    required this.status,
    required this.statusColor,
    required this.statusBackground,
    required this.note,
  });

  final int? id;
  final int? studentId;
  final String title;
  final String date;
  final String reason;
  final String status;
  final Color statusColor;
  final Color statusBackground;
  final String note;
}

class ParentNotification {
  const ParentNotification({
    required this.title,
    required this.body,
    required this.tag,
    required this.requiresReply,
    required this.color,
  });

  final String title;
  final String body;
  final String tag;
  final bool requiresReply;
  final Color color;
}

class TeacherMessage {
  const TeacherMessage({
    required this.teacher,
    required this.role,
    required this.preview,
    required this.time,
    required this.unread,
    required this.color,
  });

  final String teacher;
  final String role;
  final String preview;
  final String time;
  final bool unread;
  final Color color;
}

class TuitionBill {
  TuitionBill({
    required this.title,
    required this.amount,
    required this.dueDate,
    required this.status,
  });

  final String title;
  final int amount;
  final String dueDate;
  String status;
}

class TuitionAlert {
  TuitionAlert({
    required this.id,
    required this.studentName,
    required this.className,
    required this.message,
    required this.time,
    this.isResolved = false,
  });

  final String id;
  final String studentName;
  final String className;
  final String message;
  final String time;
  bool isResolved;
}

final List<TuitionAlert> mockTeacherTuitionAlerts = [
  TuitionAlert(
    id: 'alert-1',
    studentName: 'Nguyễn Minh Bảo',
    className: '10B2',
    message: 'Gia đình đã gửi minh chứng đóng tiền học phí qua email.',
    time: '1 giờ trước',
  ),
];

final mockStudents = [
  StudentSnapshot(
    id: 1,
    name: 'Nguyễn Minh An',
    shortName: 'MA',
    className: '12A1',
    school: 'FPT Schools Cầu Giấy',
    studentCode: 'MFS-1201',
    linkStatus: 'Active',
    homeroomTeacher: 'Cô Nguyễn Thu Hà',
    homeroomPhone: '0901 234 567',
    averageScore: 8.1,
    attendanceRate: 96,
    unreadNotifications: 3,
    pendingLeaveRequests: 1,
    aiSummary:
        'Minh An giữ chuyên cần tốt, điểm Toán tăng 0.4 so với kỳ trước. Môn Tiếng Anh cần theo dõi vì giảm nhẹ trong 2 bài gần nhất.',
    subjects: [
      SubjectScore(
        subject: 'Toán',
        score: 8.6,
        comment: 'Tăng ổn định, làm tốt bài luyện tập.',
        trend: 0.86,
        color: AppColors.green,
      ),
      SubjectScore(
        subject: 'Tiếng Anh',
        score: 7.2,
        comment: 'Cần củng cố từ vựng và bài nghe.',
        trend: 0.62,
        color: AppColors.warning,
      ),
      SubjectScore(
        subject: 'Tin học',
        score: 8.4,
        comment: 'Hoàn thành bài thực hành đúng hạn.',
        trend: 0.78,
        color: AppColors.blue,
      ),
    ],
    attendanceEvents: [
      AttendanceEvent(
        date: '10/06',
        session: 'Sáng',
        status: 'Có mặt',
        reason: 'Đã điểm danh tiết 1',
        color: AppColors.green,
        background: AppColors.greenSoft,
      ),
      AttendanceEvent(
        date: '11/06',
        session: 'Chiều',
        status: 'Muộn',
        reason: 'Muộn 8 phút, phụ huynh đã xác nhận',
        color: AppColors.warning,
        background: AppColors.warningSoft,
      ),
      AttendanceEvent(
        date: '12/06',
        session: 'Cả ngày',
        status: 'Vắng có phép',
        reason: 'Liên kết đơn nghỉ #XN-1082',
        color: AppColors.blue,
        background: AppColors.blueSoft,
      ),
    ],
    scheduleItems: [
      ScheduleItem(
        time: '07:30',
        title: 'Toán',
        detail: 'Ôn tập khảo sát cuối kỳ, phòng A204',
        icon: Icons.calculate,
        color: AppColors.green,
      ),
      ScheduleItem(
        time: '09:25',
        title: 'Tin học',
        detail: 'Thực hành Flutter, phòng Lab 3',
        icon: Icons.computer,
        color: AppColors.blue,
      ),
      ScheduleItem(
        time: '15:30',
        title: 'Họp phụ huynh trực tuyến',
        detail: 'GVCN gửi link trong thông báo lớp',
        icon: Icons.video_call,
        color: AppColors.fptOrange,
      ),
    ],
    leaveRequests: [
      LeaveRequest(
        title: 'Đơn xin nghỉ 12/06',
        date: 'Gửi 11/06/2026',
        reason: 'Khám sức khỏe định kỳ',
        status: 'Approved',
        statusColor: AppColors.success,
        statusBackground: AppColors.successSoft,
        note: 'GVCN đã duyệt và cập nhật điểm danh.',
      ),
      LeaveRequest(
        title: 'Đơn xin nghỉ 17/06',
        date: 'Gửi hôm nay',
        reason: 'Sốt nhẹ, gia đình theo dõi thêm',
        status: 'Pending',
        statusColor: AppColors.warning,
        statusBackground: AppColors.warningSoft,
        note: 'Đang chờ cô chủ nhiệm phản hồi.',
      ),
    ],
    notifications: [
      ParentNotification(
        title: 'Thông báo đóng học phí Fall 2026',
        body:
            'Nhà trường bắt đầu thu học phí học kỳ Fall 2026 từ ngày 15/06/2026.',
        tag: 'Học phí',
        requiresReply: false,
        color: AppColors.danger,
      ),
      ParentNotification(
        title: 'Xác nhận họp phụ huynh',
        body: 'Lớp 12A1 họp trực tuyến lúc 15:30 ngày 18/06.',
        tag: 'Cần xác nhận',
        requiresReply: true,
        color: AppColors.fptOrange,
      ),
      ParentNotification(
        title: 'Lịch kiểm tra Toán',
        body: 'Bài kiểm tra 45 phút diễn ra vào tiết 2 thứ sáu.',
        tag: 'Lịch kiểm tra',
        requiresReply: false,
        color: AppColors.blue,
      ),
    ],
    teacherMessages: [
      TeacherMessage(
        teacher: 'Cô Nguyễn Thu Hà',
        role: 'GVCN 12A1',
        preview: 'Cô đã nhận đơn nghỉ, gia đình theo dõi sức khỏe của An nhé.',
        time: '09:20',
        unread: true,
        color: AppColors.fptOrange,
      ),
      TeacherMessage(
        teacher: 'Thầy Trần Quốc Huy',
        role: 'Giáo viên Tin học',
        preview: 'An cần nộp lại ảnh chụp màn hình bài thực hành.',
        time: 'Hôm qua',
        unread: false,
        color: AppColors.blue,
      ),
    ],
    tuitionBills: [
      TuitionBill(
        title: 'Học phí học kỳ Fall 2026',
        amount: 12000000,
        dueDate: '30/06/2026',
        status: 'Chưa đóng',
      ),
      TuitionBill(
        title: 'Phí bán trú tháng 6/2026',
        amount: 1500000,
        dueDate: '30/06/2026',
        status: 'Chưa đóng',
      ),
      TuitionBill(
        title: 'Phí xe đưa đón tháng 6/2026',
        amount: 800000,
        dueDate: '30/06/2026',
        status: 'Đã đóng',
      ),
    ],
  ),
  StudentSnapshot(
    id: 2,
    name: 'Nguyễn Minh Bảo',
    shortName: 'MB',
    className: '10B2',
    school: 'FPT Schools Cầu Giấy',
    studentCode: 'MFS-1007',
    linkStatus: 'Active',
    homeroomTeacher: 'Thầy Lê Minh Đức',
    homeroomPhone: '0912 345 678',
    averageScore: 7.7,
    attendanceRate: 93,
    unreadNotifications: 1,
    pendingLeaveRequests: 0,
    aiSummary:
        'Minh Bảo có chuyên cần ổn định trong tháng. AI gợi ý phụ huynh tiếp tục theo dõi lịch học hằng ngày.',
    subjects: [
      SubjectScore(
        subject: 'Ngữ văn',
        score: 7.8,
        comment: 'Bài viết có tiến bộ về bố cục.',
        trend: 0.72,
        color: AppColors.teal,
      ),
      SubjectScore(
        subject: 'Khoa học',
        score: 8.0,
        comment: 'Tích cực trong hoạt động nhóm.',
        trend: 0.76,
        color: AppColors.green,
      ),
      SubjectScore(
        subject: 'Tiếng Anh',
        score: 7.1,
        comment: 'Cần luyện phát âm thêm.',
        trend: 0.58,
        color: AppColors.warning,
      ),
    ],
    attendanceEvents: [
      AttendanceEvent(
        date: '10/06',
        session: 'Sáng',
        status: 'Muộn',
        reason: 'Muộn 6 phút',
        color: AppColors.warning,
        background: AppColors.warningSoft,
      ),
      AttendanceEvent(
        date: '11/06',
        session: 'Cả ngày',
        status: 'Có mặt',
        reason: 'Đủ 8 tiết',
        color: AppColors.success,
        background: AppColors.successSoft,
      ),
      AttendanceEvent(
        date: '12/06',
        session: 'Chiều',
        status: 'Có mặt',
        reason: 'Đã điểm danh chiều',
        color: AppColors.success,
        background: AppColors.successSoft,
      ),
    ],
    scheduleItems: [
      ScheduleItem(
        time: '08:20',
        title: 'Ngữ văn',
        detail: 'Thảo luận văn bản, phòng B301',
        icon: Icons.menu_book,
        color: AppColors.teal,
      ),
      ScheduleItem(
        time: '10:15',
        title: 'Khoa học',
        detail: 'Thí nghiệm nhóm, Lab 1',
        icon: Icons.science,
        color: AppColors.green,
      ),
      ScheduleItem(
        time: '13:30',
        title: 'Vật lý',
        detail: 'Thực hành lực ma sát, phòng lý',
        icon: Icons.bolt,
        color: AppColors.blue,
      ),
    ],
    leaveRequests: [
      LeaveRequest(
        title: 'Đơn xin nghỉ 05/06',
        date: 'Gửi 04/06/2026',
        reason: 'Đi thi học sinh giỏi thành phố',
        status: 'Approved',
        statusColor: AppColors.success,
        statusBackground: AppColors.successSoft,
        note: 'Đã duyệt nghỉ phép và cập nhật điểm danh.',
      ),
    ],
    notifications: [
      ParentNotification(
        title: 'Nhắc nhở học phí tháng 6',
        body: 'Vui lòng hoàn thành học phí trước ngày 20/06.',
        tag: 'Học phí',
        requiresReply: false,
        color: AppColors.warning,
      ),
    ],
    teacherMessages: [
      TeacherMessage(
        teacher: 'Thầy Lê Minh Đức',
        role: 'GVCN 10B2',
        preview: 'Tuần này Bảo làm bài tập nhóm rất tốt.',
        time: 'Thứ sáu',
        unread: false,
        color: AppColors.green,
      ),
    ],
    tuitionBills: [
      TuitionBill(
        title: 'Học phí học kỳ Fall 2026',
        amount: 10000000,
        dueDate: '30/06/2026',
        status: 'Đã đóng',
      ),
      TuitionBill(
        title: 'Phí bán trú tháng 6/2026',
        amount: 1500000,
        dueDate: '30/06/2026',
        status: 'Đã đóng',
      ),
    ],
  ),
];
