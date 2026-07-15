import 'dart:async';

import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/school_announcement.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/announcement_inbox_service.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class AnnouncementInboxScreen extends StatefulWidget {
  const AnnouncementInboxScreen({super.key, required this.service});

  final AnnouncementInboxService service;

  @override
  State<AnnouncementInboxScreen> createState() =>
      _AnnouncementInboxScreenState();
}

class _AnnouncementInboxScreenState extends State<AnnouncementInboxScreen> {
  int? _yearId;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final selectedYear = AcademicPeriodScope.maybeOf(
      context,
    )?.selected?.academicYearId;
    if (widget.service.isTeacher &&
        selectedYear != null &&
        selectedYear != _yearId) {
      _yearId = selectedYear;
      unawaited(widget.service.setAcademicYearId(selectedYear));
    }
  }

  Future<void> _open(SchoolAnnouncement item) async {
    try {
      final detail = widget.service.isTeacher
          ? item
          : await widget.service.open(item.id);
      if (!mounted) return;
      if (widget.service.isTeacher) {
        await Navigator.of(context).push(
          MaterialPageRoute<void>(
            builder: (_) => TeacherAnnouncementRecipientsScreen(
              announcement: detail,
              service: widget.service,
            ),
          ),
        );
      } else {
        await showDialog<void>(
          context: context,
          builder: (_) => _AnnouncementDetailDialog(
            announcement: detail,
            service: widget.service,
          ),
        );
      }
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.toString())));
    }
  }

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
    animation: widget.service,
    builder: (context, _) => Scaffold(
      backgroundColor: AppColors.background,
      appBar: OrangeTopBar(
        title: widget.service.isTeacher
            ? 'Thông báo đã gửi'
            : 'Thông báo nhà trường',
        actions: [
          if (!widget.service.isTeacher &&
              widget.service.pendingActionCount > 0)
            Padding(
              key: const ValueKey('pending-action-badge'),
              padding: const EdgeInsets.only(right: 12),
              child: Center(
                child: Badge(
                  label: Text('${widget.service.pendingActionCount}'),
                  backgroundColor: AppColors.danger,
                  child: const Icon(Icons.task_alt, color: Colors.white),
                ),
              ),
            ),
        ],
      ),
      body: SafeArea(
        child: RefreshIndicator(onRefresh: widget.service.load, child: _body()),
      ),
    ),
  );

  Widget _body() {
    if (widget.service.isLoading && widget.service.announcements.isEmpty) {
      return ListView(
        children: const [
          SizedBox(height: 180),
          Center(child: CircularProgressIndicator()),
        ],
      );
    }
    if (widget.service.errorMessage != null &&
        widget.service.announcements.isEmpty) {
      return ListView(
        padding: const EdgeInsets.all(AppSpacing.lg),
        children: [
          const SizedBox(height: 100),
          const Icon(
            Icons.cloud_off_outlined,
            size: 48,
            color: AppColors.danger,
          ),
          const SizedBox(height: 12),
          Text(widget.service.errorMessage!, textAlign: TextAlign.center),
        ],
      );
    }
    if (widget.service.announcements.isEmpty) {
      return ListView(
        padding: const EdgeInsets.all(AppSpacing.lg),
        children: const [
          SizedBox(height: 100),
          Icon(Icons.notifications_none, size: 48, color: AppColors.muted),
          SizedBox(height: 12),
          Text('Chưa có thông báo nào.', textAlign: TextAlign.center),
        ],
      );
    }
    return ListView.separated(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(AppSpacing.lg),
      itemCount: widget.service.announcements.length,
      separatorBuilder: (_, _) => const SizedBox(height: AppSpacing.sm),
      itemBuilder: (_, index) {
        final item = widget.service.announcements[index];
        return widget.service.isTeacher
            ? _TeacherAnnouncementCard(item: item, onTap: () => _open(item))
            : _RecipientAnnouncementCard(item: item, onTap: () => _open(item));
      },
    );
  }
}

class _RecipientAnnouncementCard extends StatelessWidget {
  const _RecipientAnnouncementCard({required this.item, required this.onTap});
  final SchoolAnnouncement item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => AppCard(
    padding: 0,
    backgroundColor: item.isRead
        ? AppColors.surface
        : AppColors.fptOrange.withValues(alpha: .06),
    child: InkWell(
      key: ValueKey('announcement-${item.id}'),
      borderRadius: BorderRadius.circular(16),
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.md),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            CircleAvatar(
              backgroundColor: AppColors.fptOrange.withValues(alpha: .12),
              child: Icon(
                item.isRead
                    ? Icons.campaign_outlined
                    : Icons.mark_email_unread_outlined,
                color: AppColors.fptOrange,
              ),
            ),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          item.title,
                          style: const TextStyle(fontWeight: FontWeight.w800),
                        ),
                      ),
                      if (!item.isRead)
                        const CircleAvatar(
                          radius: 4,
                          backgroundColor: AppColors.danger,
                        ),
                    ],
                  ),
                  const SizedBox(height: 5),
                  Text(
                    item.body,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(color: AppColors.muted),
                  ),
                  const SizedBox(height: 9),
                  Wrap(
                    spacing: 7,
                    runSpacing: 6,
                    children: [
                      _Pill(label: item.senderName, color: AppColors.blue),
                      if (item.requiresReply)
                        _Pill(
                          label: _actionLabel(item),
                          color: item.acknowledged
                              ? AppColors.success
                              : AppColors.danger,
                        ),
                    ],
                  ),
                ],
              ),
            ),
            const Icon(Icons.chevron_right, color: AppColors.muted),
          ],
        ),
      ),
    ),
  );
}

class _TeacherAnnouncementCard extends StatelessWidget {
  const _TeacherAnnouncementCard({required this.item, required this.onTap});
  final SchoolAnnouncement item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => AppCard(
    padding: 0,
    child: InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.md),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    item.title,
                    style: const TextStyle(
                      fontWeight: FontWeight.w800,
                      fontSize: 16,
                    ),
                  ),
                ),
                _Pill(
                  label: _approvalLabel(item.approvalStatus),
                  color: item.approvalStatus == 'APPROVED'
                      ? AppColors.success
                      : AppColors.fptOrange,
                ),
              ],
            ),
            const SizedBox(height: 5),
            Text(
              item.classNames.join(', '),
              style: const TextStyle(color: AppColors.muted),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                _MiniStat(label: 'Nhận', value: item.totalRecipients),
                _MiniStat(label: 'Đọc', value: item.readCount),
                _MiniStat(label: 'Xác nhận', value: item.acknowledgedCount),
                _MiniStat(label: 'Phản hồi', value: item.repliedCount),
              ],
            ),
          ],
        ),
      ),
    ),
  );
}

class _AnnouncementDetailDialog extends StatefulWidget {
  const _AnnouncementDetailDialog({
    required this.announcement,
    required this.service,
  });
  final SchoolAnnouncement announcement;
  final AnnouncementInboxService service;

  @override
  State<_AnnouncementDetailDialog> createState() =>
      _AnnouncementDetailDialogState();
}

class _AnnouncementDetailDialogState extends State<_AnnouncementDetailDialog> {
  late SchoolAnnouncement item = widget.announcement;
  final TextEditingController replyController = TextEditingController();
  bool busy = false;

  @override
  void dispose() {
    replyController.dispose();
    super.dispose();
  }

  Future<void> acknowledge() async {
    setState(() => busy = true);
    try {
      final updated = await widget.service.acknowledge(item.id);
      if (mounted) setState(() => item = updated);
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> reply() async {
    final text = replyController.text.trim();
    if (text.isEmpty) return;
    setState(() => busy = true);
    try {
      final updated = await widget.service.reply(item.id, text);
      if (mounted) setState(() => item = updated);
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: Text(item.title),
    content: ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 520),
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(item.body),
            const SizedBox(height: 12),
            Text(
              'Gửi bởi ${item.senderName}',
              style: const TextStyle(color: AppColors.muted),
            ),
            if (item.requiresReply) ...[
              const Divider(height: 28),
              if (item.acknowledged)
                const Row(
                  children: [
                    Icon(Icons.check_circle, color: AppColors.success),
                    SizedBox(width: 7),
                    Text('Đã xác nhận đã đọc'),
                  ],
                )
              else
                FilledButton.icon(
                  key: const ValueKey('acknowledge-announcement'),
                  onPressed: busy ? null : acknowledge,
                  icon: const Icon(Icons.task_alt),
                  label: const Text('Xác nhận đã đọc'),
                ),
              const SizedBox(height: 12),
              if (item.replyText != null)
                Container(
                  key: const ValueKey('sent-announcement-reply'),
                  width: double.infinity,
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.success.withValues(alpha: .08),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text('Phản hồi đã gửi: ${item.replyText}'),
                )
              else ...[
                TextField(
                  key: const ValueKey('announcement-reply-field'),
                  controller: replyController,
                  maxLength: 1000,
                  minLines: 3,
                  maxLines: 5,
                  decoration: const InputDecoration(
                    labelText: 'Phản hồi',
                    hintText: 'Nhập nội dung phản hồi...',
                  ),
                ),
                Align(
                  alignment: Alignment.centerRight,
                  child: FilledButton(
                    key: const ValueKey('send-announcement-reply'),
                    onPressed: busy ? null : reply,
                    child: const Text('Gửi phản hồi'),
                  ),
                ),
              ],
            ],
          ],
        ),
      ),
    ),
    actions: [
      TextButton(
        onPressed: busy ? null : () => Navigator.pop(context),
        child: const Text('Đóng'),
      ),
    ],
  );
}

class TeacherAnnouncementRecipientsScreen extends StatefulWidget {
  const TeacherAnnouncementRecipientsScreen({
    super.key,
    required this.announcement,
    required this.service,
  });
  final SchoolAnnouncement announcement;
  final AnnouncementInboxService service;

  @override
  State<TeacherAnnouncementRecipientsScreen> createState() =>
      _TeacherAnnouncementRecipientsScreenState();
}

class _TeacherAnnouncementRecipientsScreenState
    extends State<TeacherAnnouncementRecipientsScreen> {
  AnnouncementRecipientPage? data;
  int? classId;
  String? status;
  bool loading = true;
  String? error;

  @override
  void initState() {
    super.initState();
    unawaited(load());
  }

  Future<void> load([int page = 0]) async {
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final result = await widget.service.api.getRecipients(
        token: widget.service.token,
        announcementId: widget.announcement.id,
        academicYearId: widget.announcement.academicYearId,
        classId: classId,
        status: status,
        page: page,
      );
      if (mounted) setState(() => data = result);
    } catch (cause) {
      if (mounted) setState(() => error = cause.toString());
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: AppColors.background,
    appBar: OrangeTopBar(title: 'Theo dõi người nhận'),
    body: SafeArea(
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(AppSpacing.md),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  widget.announcement.title,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: DropdownButtonFormField<int?>(
                        initialValue: classId,
                        decoration: const InputDecoration(labelText: 'Lớp'),
                        items: [
                          const DropdownMenuItem<int?>(
                            value: null,
                            child: Text('Tất cả lớp'),
                          ),
                          for (
                            var i = 0;
                            i < widget.announcement.classIds.length;
                            i++
                          )
                            DropdownMenuItem<int?>(
                              value: widget.announcement.classIds[i],
                              child: Text(
                                i < widget.announcement.classNames.length
                                    ? widget.announcement.classNames[i]
                                    : 'Lớp ${widget.announcement.classIds[i]}',
                              ),
                            ),
                        ],
                        onChanged: (value) {
                          setState(() => classId = value);
                          unawaited(load());
                        },
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: DropdownButtonFormField<String?>(
                        initialValue: status,
                        decoration: const InputDecoration(
                          labelText: 'Trạng thái',
                        ),
                        items: const [
                          DropdownMenuItem<String?>(
                            value: null,
                            child: Text('Tất cả'),
                          ),
                          DropdownMenuItem(
                            value: 'PENDING',
                            child: Text('Chờ hành động'),
                          ),
                          DropdownMenuItem(
                            value: 'UNREAD',
                            child: Text('Chưa đọc'),
                          ),
                          DropdownMenuItem(
                            value: 'READ',
                            child: Text('Đã đọc'),
                          ),
                          DropdownMenuItem(
                            value: 'ACKNOWLEDGED',
                            child: Text('Đã xác nhận'),
                          ),
                          DropdownMenuItem(
                            value: 'REPLIED',
                            child: Text('Đã phản hồi'),
                          ),
                        ],
                        onChanged: (value) {
                          setState(() => status = value);
                          unawaited(load());
                        },
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          if (error != null)
            Padding(
              padding: const EdgeInsets.all(12),
              child: Text(
                error!,
                style: const TextStyle(color: AppColors.danger),
              ),
            ),
          Expanded(
            child: loading && data == null
                ? const Center(child: CircularProgressIndicator())
                : ListView.separated(
                    padding: const EdgeInsets.fromLTRB(
                      AppSpacing.md,
                      0,
                      AppSpacing.md,
                      AppSpacing.md,
                    ),
                    itemCount: data?.content.length ?? 0,
                    separatorBuilder: (_, _) => const SizedBox(height: 8),
                    itemBuilder: (_, index) {
                      final recipient = data!.content[index];
                      return AppCard(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    recipient.userName,
                                    style: const TextStyle(
                                      fontWeight: FontWeight.w800,
                                    ),
                                  ),
                                ),
                                _Pill(
                                  label: _recipientStatus(recipient.status),
                                  color: recipient.status == 'UNREAD'
                                      ? AppColors.muted
                                      : AppColors.success,
                                ),
                              ],
                            ),
                            const SizedBox(height: 5),
                            Text(
                              '${recipient.studentNames.join(', ')} · ${recipient.classNames.join(', ')}',
                              style: const TextStyle(color: AppColors.muted),
                            ),
                            if (recipient.replyText != null) ...[
                              const Divider(),
                              Text('Phản hồi: ${recipient.replyText}'),
                            ],
                          ],
                        ),
                      );
                    },
                  ),
          ),
          if (data != null && data!.totalPages > 1)
            Padding(
              padding: const EdgeInsets.all(12),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  IconButton(
                    onPressed: data!.page > 0
                        ? () => load(data!.page - 1)
                        : null,
                    icon: const Icon(Icons.chevron_left),
                  ),
                  Text('${data!.page + 1}/${data!.totalPages}'),
                  IconButton(
                    onPressed: data!.page + 1 < data!.totalPages
                        ? () => load(data!.page + 1)
                        : null,
                    icon: const Icon(Icons.chevron_right),
                  ),
                ],
              ),
            ),
        ],
      ),
    ),
  );
}

class _MiniStat extends StatelessWidget {
  const _MiniStat({required this.label, required this.value});
  final String label;
  final int value;
  @override
  Widget build(BuildContext context) => Expanded(
    child: Column(
      children: [
        Text(
          '$value',
          style: const TextStyle(
            fontWeight: FontWeight.w900,
            color: AppColors.fptOrange,
          ),
        ),
        Text(
          label,
          style: const TextStyle(fontSize: 11, color: AppColors.muted),
        ),
      ],
    ),
  );
}

class _Pill extends StatelessWidget {
  const _Pill({required this.label, required this.color});
  final String label;
  final Color color;
  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
    decoration: BoxDecoration(
      color: color.withValues(alpha: .1),
      borderRadius: BorderRadius.circular(999),
    ),
    child: Text(
      label,
      style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.w700),
    ),
  );
}

String _actionLabel(SchoolAnnouncement item) {
  if (item.replyText != null) return 'Đã phản hồi';
  if (item.acknowledged) return 'Đã xác nhận';
  return 'Chờ xác nhận';
}

String _approvalLabel(String status) => switch (status) {
  'APPROVED' => 'Đã duyệt',
  'REJECTED' => 'Từ chối',
  _ => 'Chờ duyệt',
};
String _recipientStatus(String status) => switch (status) {
  'REPLIED' => 'Đã phản hồi',
  'ACKNOWLEDGED' => 'Đã xác nhận',
  'READ' => 'Đã đọc',
  _ => 'Chưa đọc',
};
