import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/activity_screen_models.dart';

// ponytail: extracted from activity_screen.dart — screen preview widgets

class ScreenMockPreview extends StatelessWidget {
  const ScreenMockPreview({super.key, required this.spec});

  final SrsScreenSpec spec;

  @override
  Widget build(BuildContext context) {
    if (spec.id.contains('05A') ||
        spec.title.contains('Import') ||
        spec.title.contains('Upload')) {
      return UploadPreview(spec: spec);
    }
    if (spec.title.contains('Báo cáo') || spec.title.contains('Xuất')) {
      return ReportPreview(spec: spec);
    }
    if (spec.title.contains('AI') || spec.title.contains('Chatbot')) {
      return AiPreview(spec: spec);
    }
    if (spec.title.contains('Điểm danh') || spec.title.contains('Duyệt')) {
      return ApprovalPreview(spec: spec);
    }
    if (spec.title.contains('Tin nhắn') ||
        spec.title.contains('Thông báo') ||
        spec.title.contains('chỉ đạo')) {
      return MessagePreview(spec: spec);
    }
    return DashboardPreview(spec: spec);
  }
}

class DashboardPreview extends StatelessWidget {
  const DashboardPreview({super.key, required this.spec});

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

class UploadPreview extends StatelessWidget {
  const UploadPreview({super.key, required this.spec});

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
          const PreviewProgress(
            label: 'Dòng hợp lệ',
            value: 0.86,
            color: AppColors.success,
          ),
          const SizedBox(height: 8),
          const PreviewProgress(
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

class ReportPreview extends StatelessWidget {
  const ReportPreview({super.key, required this.spec});

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

class AiPreview extends StatelessWidget {
  const AiPreview({super.key, required this.spec});

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

class ApprovalPreview extends StatelessWidget {
  const ApprovalPreview({super.key, required this.spec});

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

class MessagePreview extends StatelessWidget {
  const MessagePreview({super.key, required this.spec});

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

class PreviewProgress extends StatelessWidget {
  const PreviewProgress({
    super.key,
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
