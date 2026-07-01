import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/actor_models.dart';

// ponytail: extracted from activity_screen.dart — data models and constants

class CatalogGroup {
  const CatalogGroup({
    required this.label,
    required this.icon,
    required this.color,
    required this.screens,
  });

  final String label;
  final IconData icon;
  final Color color;
  final List<SrsScreenSpec> screens;

  String get ucCountLabel {
    final ids = <String>{};
    for (final screen in screens) {
      ids.addAll(screen.ucs.split(',').map((uc) => uc.trim()));
    }
    return ids.length.toString();
  }
}

class SrsScreenSpec {
  const SrsScreenSpec({
    required this.id,
    required this.title,
    required this.actorLabel,
    required this.ucs,
    required this.channel,
    required this.components,
    required this.flow,
    required this.icon,
    required this.color,
  });

  final String id;
  final String title;
  final String actorLabel;
  final String ucs;
  final String channel;
  final List<String> components;
  final List<String> flow;
  final IconData icon;
  final Color color;
}

int groupIndexFor(AppActor actor) {
  switch (actor) {
    case AppActor.parent:
      return 1;
    case AppActor.teacher:
      return 2;
    case AppActor.student:
      return 1;
  }
}

const catalogGroups = [
  CatalogGroup(
    label: 'Chung',
    icon: Icons.apps,
    color: AppColors.fptOrange,
    screens: commonScreens,
  ),
  CatalogGroup(
    label: 'Phụ huynh',
    icon: Icons.family_restroom,
    color: AppColors.fptOrange,
    screens: parentScreens,
  ),
  CatalogGroup(
    label: 'Giáo viên',
    icon: Icons.co_present,
    color: AppColors.blue,
    screens: teacherScreens,
  ),
];

const _commonFlow = [
  'Người dùng mở màn hình và hệ thống kiểm tra phiên đăng nhập.',
  'Hệ thống xác định vai trò, scope dữ liệu và quyền thao tác.',
  'Người dùng nhập, lọc hoặc chọn dữ liệu cần xử lý.',
  'Hệ thống validate, phản hồi trạng thái và ghi audit khi cần.',
];

const commonScreens = [
  SrsScreenSpec(
    id: 'SCR-COM-01',
    title: 'Đăng nhập/OTP',
    actorLabel: 'Tất cả actor',
    ucs: 'UC-01',
    channel: 'Mobile + Web',
    components: ['SĐT/email', 'Mật khẩu/OTP', 'Quên mật khẩu', 'Chọn vai trò'],
    flow: [
      'Nhập SĐT/email.',
      'Nhận OTP hoặc nhập mật khẩu.',
      'Xác thực tài khoản.',
      'Chọn vai trò nếu có nhiều vai trò.',
    ],
    icon: Icons.login,
    color: AppColors.fptOrange,
  ),
  SrsScreenSpec(
    id: 'SCR-COM-02',
    title: 'Thông tin tài khoản',
    actorLabel: 'Tất cả actor',
    ucs: 'UC-01',
    channel: 'Mobile + Web',
    components: ['Hồ sơ', 'Đổi mật khẩu', 'Thiết bị đăng nhập', 'Đăng xuất'],
    flow: _commonFlow,
    icon: Icons.account_circle_outlined,
    color: AppColors.blue,
  ),
  SrsScreenSpec(
    id: 'SCR-COM-03',
    title: 'Trung tâm thông báo',
    actorLabel: 'Tất cả actor',
    ucs: 'UC-07, UC-12, UC-18',
    channel: 'Mobile + Web',
    components: ['Danh sách thông báo', 'Filter', 'Trạng thái đọc', 'Phản hồi'],
    flow: [
      'Mở danh sách thông báo.',
      'Lọc theo chưa đọc/cần phản hồi.',
      'Mở chi tiết hoặc file đính kèm.',
      'Xác nhận đọc/phản hồi nếu được phép.',
    ],
    icon: Icons.notifications_active_outlined,
    color: AppColors.warning,
  ),
  SrsScreenSpec(
    id: 'SCR-COM-04',
    title: 'File Viewer',
    actorLabel: 'Tất cả actor',
    ucs: 'UC-07, UC-18',
    channel: 'Mobile + Web',
    components: ['Xem ảnh/PDF', 'Kiểm quyền', 'Watermark', 'Audit'],
    flow: [
      'Người dùng mở file.',
      'Hệ thống kiểm tra quyền theo scope.',
      'Hiển thị file có watermark.',
      'Ghi log truy cập dữ liệu nhạy cảm.',
    ],
    icon: Icons.attach_file,
    color: AppColors.green,
  ),
  SrsScreenSpec(
    id: 'SCR-COM-05',
    title: 'Chatbot AI thống kê',
    actorLabel: 'Tất cả actor',
    ucs: 'UC-19',
    channel: 'Mobile summary + Web detail',
    components: [
      'Hỏi đáp thống kê',
      'Nguồn dữ liệu',
      'Deep link',
      'Guardrail RBAC',
    ],
    flow: [
      'Nhập câu hỏi tiếng Việt.',
      'AI phân loại intent và scope.',
      'Statistics API trả dữ liệu được phép.',
      'AI trả lời kèm bộ lọc và deep link.',
    ],
    icon: Icons.auto_awesome,
    color: AppColors.fptOrange,
  ),
];

const parentScreens = [
  SrsScreenSpec(
    id: 'SCR-PH-01',
    title: 'Dashboard phụ huynh',
    actorLabel: 'Phụ huynh',
    ucs: 'UC-03, UC-04, UC-05, UC-06, UC-07, UC-08',
    channel: 'Mobile primary',
    components: [
      'Thẻ học sinh',
      'Điểm mới',
      'Chuyên cần',
      'Thông báo',
      'Xin nghỉ',
    ],
    flow: [
      'Chọn học sinh liên kết.',
      'Xem điểm/chuyên cần/thông báo mới.',
      'Mở tác vụ nhanh.',
      'Theo dõi trạng thái phản hồi.',
    ],
    icon: Icons.dashboard_outlined,
    color: AppColors.fptOrange,
  ),
  SrsScreenSpec(
    id: 'SCR-PH-02',
    title: 'Chọn học sinh',
    actorLabel: 'Phụ huynh',
    ucs: 'UC-03',
    channel: 'Mobile primary',
    components: ['Danh sách con', 'Trạng thái xác minh', 'Lớp', 'GVCN'],
    flow: [
      'Tải học sinh đã liên kết.',
      'Hiển thị Active/Pending.',
      'Chọn học sinh.',
      'Cập nhật dashboard theo học sinh.',
    ],
    icon: Icons.switch_account,
    color: AppColors.fptOrange,
  ),
  SrsScreenSpec(
    id: 'SCR-PH-03',
    title: 'Hồ sơ học sinh',
    actorLabel: 'Phụ huynh',
    ucs: 'UC-03',
    channel: 'Mobile primary',
    components: ['Thông tin con', 'Lớp', 'GVCN', 'Giáo viên bộ môn'],
    flow: _commonFlow,
    icon: Icons.badge_outlined,
    color: AppColors.blue,
  ),
  SrsScreenSpec(
    id: 'SCR-PH-04',
    title: 'Điểm & nhận xét',
    actorLabel: 'Phụ huynh',
    ucs: 'UC-04',
    channel: 'Mobile primary',
    components: ['Bộ lọc kỳ/môn', 'Bảng điểm', 'Nhận xét', 'Biểu đồ xu hướng'],
    flow: [
      'Chọn học kỳ/môn.',
      'Hệ thống tải điểm đã công bố.',
      'Xem nhận xét giáo viên.',
      'AI giải thích xu hướng nếu hỏi.',
    ],
    icon: Icons.fact_check_outlined,
    color: AppColors.teal,
  ),
  SrsScreenSpec(
    id: 'SCR-PH-05',
    title: 'Chuyên cần',
    actorLabel: 'Phụ huynh',
    ucs: 'UC-05',
    channel: 'Mobile primary',
    components: ['Lịch trạng thái', 'Vắng/muộn', 'Lý do', 'Liên kết đơn nghỉ'],
    flow: [
      'Chọn tháng/tuần.',
      'Xem trạng thái theo ngày/buổi.',
      'Mở lý do hoặc đơn liên quan.',
      'Gửi trao đổi nếu cần.',
    ],
    icon: Icons.event_available_outlined,
    color: AppColors.green,
  ),
  SrsScreenSpec(
    id: 'SCR-PH-06',
    title: 'Lịch học/Sự kiện',
    actorLabel: 'Phụ huynh',
    ucs: 'UC-03',
    channel: 'Mobile primary',
    components: ['Thời khóa biểu', 'Lịch kiểm tra', 'Sự kiện trường/lớp'],
    flow: _commonFlow,
    icon: Icons.calendar_month_outlined,
    color: AppColors.blue,
  ),
  SrsScreenSpec(
    id: 'SCR-PH-07',
    title: 'Danh sách đơn xin nghỉ',
    actorLabel: 'Phụ huynh',
    ucs: 'UC-06',
    channel: 'Mobile primary',
    components: ['Pending', 'Approved', 'Rejected', 'Lịch sử'],
    flow: [
      'Mở danh sách đơn.',
      'Lọc theo trạng thái.',
      'Xem chi tiết/ý kiến giáo viên.',
      'Tạo đơn mới nếu cần.',
    ],
    icon: Icons.assignment_outlined,
    color: AppColors.warning,
  ),
  SrsScreenSpec(
    id: 'SCR-PH-08',
    title: 'Tạo đơn xin nghỉ',
    actorLabel: 'Phụ huynh',
    ucs: 'UC-06',
    channel: 'Mobile primary',
    components: ['Ngày/buổi', 'Lý do', 'Đính kèm', 'Gửi đơn'],
    flow: [
      'Chọn học sinh, ngày và buổi nghỉ.',
      'Nhập lý do và đính kèm minh chứng.',
      'Hệ thống kiểm trùng/ngày quá khứ/quyền.',
      'Tạo đơn Pending và báo giáo viên.',
    ],
    icon: Icons.note_add_outlined,
    color: AppColors.fptOrange,
  ),
  SrsScreenSpec(
    id: 'SCR-PH-09',
    title: 'Tin nhắn giáo viên',
    actorLabel: 'Phụ huynh',
    ucs: 'UC-08',
    channel: 'Mobile primary',
    components: ['Thread', 'Gửi tin', 'Đính kèm', 'Kiểm soát nội dung'],
    flow: _commonFlow,
    icon: Icons.chat_bubble_outline,
    color: AppColors.violet,
  ),
];

const teacherScreens = [
  SrsScreenSpec(
    id: 'SCR-GV-01',
    title: 'Dashboard giáo viên',
    actorLabel: 'Giáo viên',
    ucs: 'UC-09, UC-10, UC-11, UC-12, UC-13',
    channel: 'Mobile + Web',
    components: ['Lớp được phân công', 'Việc cần làm', 'Cảnh báo lớp'],
    flow: [
      'Tải lớp/môn được phân công.',
      'Hiển thị việc cần làm.',
      'Mở điểm danh/duyệt đơn/nhập điểm.',
      'Theo dõi cảnh báo AI lớp.',
    ],
    icon: Icons.dashboard_outlined,
    color: AppColors.blue,
  ),
  SrsScreenSpec(
    id: 'SCR-GV-02',
    title: 'Danh sách lớp',
    actorLabel: 'Giáo viên',
    ucs: 'UC-13',
    channel: 'Mobile + Web',
    components: ['Lớp/môn', 'Sĩ số', 'Phím tắt điểm danh/điểm'],
    flow: _commonFlow,
    icon: Icons.groups_outlined,
    color: AppColors.blue,
  ),
  SrsScreenSpec(
    id: 'SCR-GV-03',
    title: 'Điểm danh',
    actorLabel: 'Giáo viên',
    ucs: 'UC-09',
    channel: 'Mobile + Web',
    components: ['Roster', 'Trạng thái nhanh', 'Ghi chú', 'Gửi thông báo'],
    flow: [
      'Chọn lớp, ngày, buổi/tiết.',
      'Tải danh sách học sinh.',
      'Đánh dấu có mặt/vắng/muộn.',
      'Lưu điểm danh và ghi audit.',
    ],
    icon: Icons.checklist_outlined,
    color: AppColors.fptOrange,
  ),
  SrsScreenSpec(
    id: 'SCR-GV-04',
    title: 'Duyệt đơn xin nghỉ',
    actorLabel: 'Giáo viên',
    ucs: 'UC-10',
    channel: 'Mobile + Web',
    components: ['Danh sách đơn', 'Chi tiết', 'Duyệt/từ chối', 'Lý do'],
    flow: [
      'Mở danh sách đơn Pending.',
      'Xem lý do và minh chứng.',
      'Duyệt hoặc từ chối kèm lý do.',
      'Thông báo phụ huynh và cập nhật chuyên cần.',
    ],
    icon: Icons.fact_check_outlined,
    color: AppColors.warning,
  ),
  SrsScreenSpec(
    id: 'SCR-GV-05',
    title: 'Nhập điểm web',
    actorLabel: 'Giáo viên',
    ucs: 'UC-11',
    channel: 'Web primary',
    components: ['Bảng điểm web', 'Cột điểm', 'Validate', 'Lưu nháp/công bố'],
    flow: [
      'Chọn lớp, môn, học kỳ, cột điểm.',
      'Nhập điểm trực tiếp.',
      'Validate thang điểm/kỳ mở điểm.',
      'Lưu nháp hoặc công bố.',
    ],
    icon: Icons.edit_note,
    color: AppColors.teal,
  ),
  SrsScreenSpec(
    id: 'SCR-GV-05A',
    title: 'Upload file điểm web',
    actorLabel: 'Giáo viên',
    ucs: 'UC-11',
    channel: 'Web only',
    components: [
      'Tải template',
      'Upload Excel',
      'Preview lỗi',
      'Import/rollback',
    ],
    flow: [
      'Tải template đúng lớp/môn/cột điểm.',
      'Upload Excel đã điền.',
      'Preview dòng hợp lệ/lỗi.',
      'Xác nhận import hoặc rollback.',
    ],
    icon: Icons.upload_file,
    color: AppColors.fptOrange,
  ),
  SrsScreenSpec(
    id: 'SCR-GV-06',
    title: 'Nhập nhận xét',
    actorLabel: 'Giáo viên',
    ucs: 'UC-11',
    channel: 'Web primary',
    components: ['Nhận xét học sinh', 'Nhận xét nhóm', 'Lưu nháp', 'Công bố'],
    flow: _commonFlow,
    icon: Icons.rate_review_outlined,
    color: AppColors.green,
  ),
  SrsScreenSpec(
    id: 'SCR-GV-07',
    title: 'Tạo thông báo lớp',
    actorLabel: 'Giáo viên',
    ucs: 'UC-12',
    channel: 'Mobile + Web',
    components: [
      'Soạn nội dung',
      'Chọn người nhận',
      'Gửi/phê duyệt',
      'Tracking đọc',
    ],
    flow: [
      'Chọn lớp/nhóm/học sinh.',
      'Soạn nội dung và file.',
      'Gửi hoặc chuyển phê duyệt.',
      'Theo dõi delivered/read.',
    ],
    icon: Icons.campaign_outlined,
    color: AppColors.blue,
  ),
  SrsScreenSpec(
    id: 'SCR-GV-08',
    title: 'Tin nhắn phụ huynh',
    actorLabel: 'Giáo viên',
    ucs: 'UC-08',
    channel: 'Mobile + Web',
    components: ['Inbox theo lớp', 'Theo học sinh', 'Ưu tiên', 'Đính kèm'],
    flow: _commonFlow,
    icon: Icons.forum_outlined,
    color: AppColors.violet,
  ),
  SrsScreenSpec(
    id: 'SCR-GV-09',
    title: 'Thống kê lớp',
    actorLabel: 'Giáo viên',
    ucs: 'UC-20',
    channel: 'Mobile summary + Web detail',
    components: ['Chuyên cần', 'Điểm', 'Phụ huynh chưa đọc', 'AI cảnh báo'],
    flow: [
      'Chọn lớp/kỳ.',
      'Tổng hợp điểm, chuyên cần, thông báo.',
      'AI so sánh kỳ trước và phát hiện bất thường.',
      'Mở danh sách học sinh cần chú ý.',
    ],
    icon: Icons.insights_outlined,
    color: AppColors.fptOrange,
  ),
];
