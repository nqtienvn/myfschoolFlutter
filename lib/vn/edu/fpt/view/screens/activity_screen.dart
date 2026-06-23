import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/actor_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class ActivityScreen extends StatefulWidget {
  const ActivityScreen({super.key, this.activeActor = AppActor.parent});

  final AppActor activeActor;

  @override
  State<ActivityScreen> createState() => _ActivityScreenState();
}

class _ActivityScreenState extends State<ActivityScreen> {
  late int _selectedGroup = _groupIndexFor(widget.activeActor);

  @override
  Widget build(BuildContext context) {
    final group = _catalogGroups[_selectedGroup];

    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(18, 18, 18, 0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          color: AppColors.primarySoft,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: const Icon(
                          Icons.account_tree_outlined,
                          color: AppColors.fptOrange,
                        ),
                      ),
                      const SizedBox(width: 12),
                      const Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Catalog màn hình',
                              style: TextStyle(
                                fontSize: 24,
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                            SizedBox(height: 4),
                            Text(
                              'Screen ID, UC trace, flow, AC và UI preview từ SRS v3.',
                              style: TextStyle(
                                color: AppColors.muted,
                                fontSize: 12,
                                height: 1.3,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        for (var i = 0; i < _catalogGroups.length; i++)
                          Padding(
                            padding: const EdgeInsets.only(right: 8),
                            child: ChoiceChip(
                              selected: _selectedGroup == i,
                              label: Text(_catalogGroups[i].label),
                              avatar: Icon(_catalogGroups[i].icon, size: 18),
                              onSelected: (_) => setState(() => _selectedGroup = i),
                              selectedColor: AppColors.primarySoft,
                              labelStyle: TextStyle(
                                color: _selectedGroup == i
                                    ? AppColors.fptOrange
                                    : AppColors.ink,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                ],
              ),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 6),
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: StatTile(
                          label: 'Màn hình',
                          value: group.screens.length.toString(),
                          icon: Icons.dashboard_customize_outlined,
                          color: group.color,
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: StatTile(
                          label: 'UC liên quan',
                          value: group.ucCountLabel,
                          icon: Icons.account_tree_outlined,
                          color: AppColors.fptOrange,
                        ),
                      ),
                    ],
                  ),
                  const SectionHeader(title: 'Danh sách màn hình'),
                  for (final spec in group.screens)
                    _ScreenSpecCard(
                      spec: spec,
                      onTap: () {
                        Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (_) => SrsScreenDetailScreen(spec: spec),
                          ),
                        );
                      },
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class SrsScreenDetailScreen extends StatelessWidget {
  const SrsScreenDetailScreen({super.key, required this.spec});

  final SrsScreenSpec spec;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: OrangeTopBar(title: spec.id),
      body: ListView(
        padding: const EdgeInsets.all(18),
        children: [
          SurfacePanel(
            color: spec.color.withValues(alpha: 0.08),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: spec.color.withValues(alpha: 0.16),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Icon(spec.icon, color: spec.color),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        spec.title,
                        style: const TextStyle(
                          color: AppColors.ink,
                          fontSize: 20,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        '${spec.actorLabel} • ${spec.channel}',
                        style: const TextStyle(
                          color: AppColors.muted,
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 8),
                      StatusPill(
                        label: spec.ucs,
                        foreground: spec.color,
                        background: Colors.white,
                        compact: true,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SectionHeader(title: 'Thành phần chính'),
          SurfacePanel(
            child: Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final component in spec.components)
                  StatusPill(
                    label: component,
                    foreground: AppColors.ink,
                    background: AppColors.background,
                    compact: true,
                  ),
              ],
            ),
          ),
          const SectionHeader(title: 'Flow / activity'),
          SurfacePanel(
            child: Column(
              children: [
                for (var i = 0; i < spec.flow.length; i++)
                  _FlowStep(
                    index: i + 1,
                    text: spec.flow[i],
                    isLast: i == spec.flow.length - 1,
                    color: spec.color,
                  ),
              ],
            ),
          ),
          const SectionHeader(title: 'Acceptance detail'),
          const SurfacePanel(
            child: Column(
              children: [
                _AcceptanceLine(
                  text:
                      'Kiểm tra quyền RBAC và phạm vi dữ liệu trước khi hiển thị.',
                ),
                _AcceptanceLine(
                  text:
                      'Validate trường bắt buộc, hiển thị lỗi inline và giữ draft khi phù hợp.',
                ),
                _AcceptanceLine(
                  text:
                      'Pending/Approved/Rejected hoặc job/export phải có trạng thái rõ ràng.',
                ),
                _AcceptanceLine(
                  text:
                      'Tác vụ nhạy cảm phải có dấu hiệu audit, watermark hoặc nguồn dữ liệu.',
                ),
              ],
            ),
          ),
          const SectionHeader(title: 'UI preview'),
          _ScreenMockPreview(spec: spec),
        ],
      ),
    );
  }
}

class _ScreenSpecCard extends StatelessWidget {
  const _ScreenSpecCard({required this.spec, required this.onTap});

  final SrsScreenSpec spec;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        borderRadius: BorderRadius.circular(8),
        onTap: onTap,
        child: SurfacePanel(
          child: Row(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: spec.color.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Icon(spec.icon, color: spec.color),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        StatusPill(
                          label: spec.id,
                          foreground: spec.color,
                          background: spec.color.withValues(alpha: 0.12),
                          compact: true,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            spec.ucs,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              color: AppColors.muted,
                              fontSize: 11,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text(
                      spec.title,
                      style: const TextStyle(
                        color: AppColors.ink,
                        fontSize: 14,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      spec.components.take(3).join(' • '),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: AppColors.muted,
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: AppColors.quiet),
            ],
          ),
        ),
      ),
    );
  }
}

class _FlowStep extends StatelessWidget {
  const _FlowStep({
    required this.index,
    required this.text,
    required this.isLast,
    required this.color,
  });

  final int index;
  final String text;
  final bool isLast;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Column(
          children: [
            CircleAvatar(
              radius: 14,
              backgroundColor: color.withValues(alpha: 0.14),
              child: Text(
                '$index',
                style: TextStyle(
                  color: color,
                  fontSize: 12,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ),
            if (!isLast) Container(width: 1, height: 28, color: AppColors.line),
          ],
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Padding(
            padding: EdgeInsets.only(top: 5, bottom: isLast ? 0 : 12),
            child: Text(
              text,
              style: const TextStyle(
                color: AppColors.ink,
                fontSize: 13,
                height: 1.35,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _AcceptanceLine extends StatelessWidget {
  const _AcceptanceLine({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.check_circle, color: AppColors.success, size: 18),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(
                color: AppColors.ink,
                fontSize: 13,
                height: 1.35,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ScreenMockPreview extends StatelessWidget {
  const _ScreenMockPreview({required this.spec});

  final SrsScreenSpec spec;

  @override
  Widget build(BuildContext context) {
    if (spec.id.contains('05A') ||
        spec.title.contains('Import') ||
        spec.title.contains('Upload')) {
      return _UploadPreview(spec: spec);
    }
    if (spec.title.contains('Báo cáo') || spec.title.contains('Xuất')) {
      return _ReportPreview(spec: spec);
    }
    if (spec.title.contains('AI') || spec.title.contains('Chatbot')) {
      return _AiPreview(spec: spec);
    }
    if (spec.title.contains('Điểm danh') || spec.title.contains('Duyệt')) {
      return _ApprovalPreview(spec: spec);
    }
    if (spec.title.contains('Tin nhắn') ||
        spec.title.contains('Thông báo') ||
        spec.title.contains('chỉ đạo')) {
      return _MessagePreview(spec: spec);
    }
    return _DashboardPreview(spec: spec);
  }
}

class _DashboardPreview extends StatelessWidget {
  const _DashboardPreview({required this.spec});

  final SrsScreenSpec spec;

  @override
  Widget build(BuildContext context) {
    return SurfacePanel(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: StatTile(
                  label: 'Hoàn thành',
                  value: '92%',
                  icon: Icons.task_alt,
                  color: spec.color,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: StatTile(
                  label: 'Cảnh báo',
                  value: '4',
                  icon: Icons.warning_amber,
                  color: AppColors.warning,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          const Text(
            'Bộ lọc',
            style: TextStyle(color: AppColors.ink, fontWeight: FontWeight.w900),
          ),
          const SizedBox(height: 8),
          const Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              StatusPill(label: '2026 - 2027', compact: true),
              StatusPill(label: 'Học kỳ II', compact: true),
              StatusPill(label: 'Tháng này', compact: true),
            ],
          ),
        ],
      ),
    );
  }
}

class _UploadPreview extends StatelessWidget {
  const _UploadPreview({required this.spec});

  final SrsScreenSpec spec;

  @override
  Widget build(BuildContext context) {
    return SurfacePanel(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.cloud_upload_outlined, color: spec.color),
              const SizedBox(width: 10),
              const Expanded(
                child: Text(
                  'Tải template, upload Excel, preview lỗi và xác nhận import.',
                  style: TextStyle(fontWeight: FontWeight.w800, height: 1.3),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          const _PreviewProgress(
            label: 'Dòng hợp lệ',
            value: 0.86,
            color: AppColors.success,
          ),
          const SizedBox(height: 8),
          const _PreviewProgress(
            label: 'Dòng lỗi',
            value: 0.14,
            color: AppColors.danger,
          ),
          const Divider(height: 22),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.download),
                  label: const Text('Template'),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.check),
                  label: const Text('Import'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _ReportPreview extends StatelessWidget {
  const _ReportPreview({required this.spec});

  final SrsScreenSpec spec;

  @override
  Widget build(BuildContext context) {
    return SurfacePanel(
      child: Column(
        children: [
          TextField(
            decoration: InputDecoration(
              labelText: 'Loại báo cáo',
              prefixIcon: Icon(Icons.filter_alt_outlined, color: spec.color),
            ),
          ),
          const SizedBox(height: 10),
          const TextField(
            decoration: InputDecoration(
              labelText: 'Phạm vi / thời gian',
              prefixIcon: Icon(Icons.date_range_outlined),
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.table_view),
                  label: const Text('Excel'),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.picture_as_pdf),
                  label: const Text('PDF'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          const StatusPill(
            label: 'Watermark + background job + audit log',
            foreground: AppColors.fptOrange,
            background: AppColors.primarySoft,
          ),
        ],
      ),
    );
  }
}

class _AiPreview extends StatelessWidget {
  const _AiPreview({required this.spec});

  final SrsScreenSpec spec;

  @override
  Widget build(BuildContext context) {
    return SurfacePanel(
      color: AppColors.orangePale,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(Icons.auto_awesome, color: spec.color),
              const SizedBox(width: 10),
              const Expanded(
                child: Text(
                  'AI trả lời bằng số liệu tổng hợp trong phạm vi RBAC, nêu nguồn/bộ lọc và mở deep link liên quan.',
                  style: TextStyle(
                    color: AppColors.ink,
                    height: 1.35,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          const TextField(
            decoration: InputDecoration(
              labelText: 'Hỏi thống kê hoặc yêu cầu mở tính năng',
              prefixIcon: Icon(Icons.chat_bubble_outline),
              suffixIcon: Icon(Icons.send),
            ),
          ),
        ],
      ),
    );
  }
}

class _ApprovalPreview extends StatelessWidget {
  const _ApprovalPreview({required this.spec});

  final SrsScreenSpec spec;

  @override
  Widget build(BuildContext context) {
    return SurfacePanel(
      child: Column(
        children: [
          Row(
            children: [
              CircleAvatar(
                backgroundColor: spec.color.withValues(alpha: 0.12),
                child: Icon(spec.icon, color: spec.color),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Nguyễn Minh An - 12A',
                      style: TextStyle(fontWeight: FontWeight.w900),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Chờ xử lý, có ghi chú và audit sau khi lưu.',
                      style: TextStyle(color: AppColors.muted, fontSize: 12),
                    ),
                  ],
                ),
              ),
              const StatusPill(
                label: 'Pending',
                foreground: AppColors.warning,
                background: AppColors.warningSoft,
                compact: true,
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.close),
                  label: const Text('Từ chối'),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.check),
                  label: const Text('Duyệt/Lưu'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _MessagePreview extends StatelessWidget {
  const _MessagePreview({required this.spec});

  final SrsScreenSpec spec;

  @override
  Widget build(BuildContext context) {
    return SurfacePanel(
      child: Column(
        children: [
          Row(
            children: [
              Icon(Icons.campaign_outlined, color: spec.color),
              const SizedBox(width: 10),
              const Expanded(
                child: Text(
                  'Soạn nội dung, chọn người nhận, gửi/phê duyệt và theo dõi đã đọc.',
                  style: TextStyle(fontWeight: FontWeight.w800, height: 1.3),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          const TextField(
            minLines: 2,
            maxLines: 3,
            decoration: InputDecoration(
              labelText: 'Nội dung',
              prefixIcon: Icon(Icons.edit_note),
            ),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              const Expanded(
                child: StatusPill(
                  label: 'Đã đọc 78%',
                  foreground: AppColors.success,
                  background: AppColors.successSoft,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.send),
                  label: const Text('Gửi'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _PreviewProgress extends StatelessWidget {
  const _PreviewProgress({
    required this.label,
    required this.value,
    required this.color,
  });

  final String label;
  final double value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        SizedBox(
          width: 82,
          child: Text(
            label,
            style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w800),
          ),
        ),
        Expanded(
          child: ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: LinearProgressIndicator(
              minHeight: 8,
              value: value,
              backgroundColor: color.withValues(alpha: 0.12),
              color: color,
            ),
          ),
        ),
        const SizedBox(width: 8),
        Text(
          '${(value * 100).round()}%',
          style: TextStyle(
            color: color,
            fontSize: 12,
            fontWeight: FontWeight.w900,
          ),
        ),
      ],
    );
  }
}

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

int _groupIndexFor(AppActor actor) {
  switch (actor) {
    case AppActor.parent:
      return 1;
    case AppActor.teacher:
      return 2;
    case AppActor.student:
      return 1;
  }
}

const _catalogGroups = [
  CatalogGroup(
    label: 'Chung',
    icon: Icons.apps,
    color: AppColors.fptOrange,
    screens: _commonScreens,
  ),
  CatalogGroup(
    label: 'Phụ huynh',
    icon: Icons.family_restroom,
    color: AppColors.fptOrange,
    screens: _parentScreens,
  ),
  CatalogGroup(
    label: 'Giáo viên',
    icon: Icons.co_present,
    color: AppColors.blue,
    screens: _teacherScreens,
  ),
];

const _commonFlow = [
  'Người dùng mở màn hình và hệ thống kiểm tra phiên đăng nhập.',
  'Hệ thống xác định vai trò, scope dữ liệu và quyền thao tác.',
  'Người dùng nhập, lọc hoặc chọn dữ liệu cần xử lý.',
  'Hệ thống validate, phản hồi trạng thái và ghi audit khi cần.',
];

const _commonScreens = [
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

const _parentScreens = [
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

const _teacherScreens = [
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

