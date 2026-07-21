import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class AnnouncementsCreateScreen extends StatefulWidget {
  const AnnouncementsCreateScreen({super.key, required this.token, this.api});

  final String token;
  final BackendApiClient? api;

  @override
  State<AnnouncementsCreateScreen> createState() =>
      _AnnouncementsCreateScreenState();
}

class _AnnouncementsCreateScreenState extends State<AnnouncementsCreateScreen> {
  final _title = TextEditingController();
  final _body = TextEditingController();
  late final BackendApiClient _api;
  List<Map<String, dynamic>> _classes = const [];
  List<Map<String, dynamic>> _sent = const [];
  final Set<int> _classIds = {};
  String _target = 'ALL';
  int? _retryOfAnnouncementId;
  bool _loading = true;
  bool _sending = false;
  int? _yearId;

  @override
  void initState() {
    super.initState();
    _api = widget.api ?? BackendApiClient();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final id = AcademicPeriodScope.maybeOf(context)?.selected?.academicYearId;
    if (id != null && id != _yearId) {
      _yearId = id;
      _resetDraft();
      _load();
    }
  }

  @override
  void dispose() {
    _title.dispose();
    _body.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final results = await Future.wait([
        _api.getData(
          '/api/announcements/eligible-classes',
          token: widget.token,
          query: {'academicYearId': '$_yearId'},
        ),
        _api.getData(
          '/api/announcements/mine',
          token: widget.token,
          query: {'academicYearId': '$_yearId'},
        ),
      ]);
      if (!mounted) return;
      setState(() {
        _classes = (results[0] as List? ?? const [])
            .whereType<Map<String, dynamic>>()
            .toList();
        _sent = (results[1] as List? ?? const [])
            .whereType<Map<String, dynamic>>()
            .where((item) => item['academicYearId'] == _yearId)
            .toList();
      });
    } catch (error) {
      _message(error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _save() async {
    if (_title.text.trim().isEmpty ||
        _body.text.trim().isEmpty ||
        _classIds.isEmpty) {
      _message('Vui lòng nhập nội dung và chọn ít nhất một lớp.');
      return;
    }
    setState(() => _sending = true);
    try {
      final payload = <String, dynamic>{
        'title': _title.text.trim(),
        'body': _body.text.trim(),
        'targetRole': _target,
        'academicYearId': _yearId,
        'classIds': _classIds.toList(),
        if (_retryOfAnnouncementId != null)
          'retryOfAnnouncementId': _retryOfAnnouncementId,
      };
      final raw = await _api.postData(
        '/api/announcements',
        token: widget.token,
        body: payload,
      );
      if (raw is! Map<String, dynamic>) {
        throw const FormatException('Kết quả kiểm tra thông báo không hợp lệ.');
      }
      final outcome = raw['outcome'] as String?;
      final message =
          raw['message'] as String? ??
          'Không nhận được kết quả kiểm tra nội dung.';
      final announcement = raw['announcement'];
      if (outcome != 'PUBLISHED' && outcome != 'SYSTEM_REJECTED' ||
          announcement is! Map<String, dynamic>) {
        throw const FormatException('Kết quả kiểm tra thông báo không hợp lệ.');
      }
      final violations = (raw['violations'] as List? ?? const [])
          .whereType<Map<String, dynamic>>()
          .toList();
      if (!mounted) return;
      setState(() {
        _sending = false;
        _sent = [announcement, ..._sent];
        if (outcome == 'PUBLISHED') {
          _title.clear();
          _body.clear();
          _target = 'ALL';
          _classIds.clear();
          _retryOfAnnouncementId = null;
        } else {
          _retryOfAnnouncementId = announcement['id'] as int?;
        }
      });
      await _showResult(
        published: outcome == 'PUBLISHED',
        message: message,
        violations: violations,
      );
    } catch (error) {
      _message(error.toString());
    } finally {
      if (mounted && _sending) setState(() => _sending = false);
    }
  }

  Future<void> _showResult({
    required bool published,
    required String message,
    required List<Map<String, dynamic>> violations,
  }) {
    return showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => AlertDialog(
        key: ValueKey(
          published
              ? 'announcement-published-dialog'
              : 'announcement-rejected-dialog',
        ),
        icon: CircleAvatar(
          radius: 28,
          backgroundColor: published ? AppColors.green : AppColors.danger,
          child: Icon(
            published ? Icons.check_rounded : Icons.priority_high_rounded,
            color: Colors.white,
            size: 32,
          ),
        ),
        title: Text(
          published
              ? 'Gửi thông báo thành công'
              : 'Thông báo bị hệ thống từ chối',
          textAlign: TextAlign.center,
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(message, textAlign: TextAlign.center),
            if (violations.isNotEmpty) ...[
              const SizedBox(height: 14),
              const Text(
                'Câu từ vi phạm:',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 6),
              ...violations.map(
                (violation) => Padding(
                  padding: const EdgeInsets.only(bottom: 4),
                  child: Text(
                    '• ${_fieldLabel(violation['field'])}: “${violation['phrase']}”',
                    style: const TextStyle(color: AppColors.danger),
                  ),
                ),
              ),
            ],
          ],
        ),
        actions: [
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(published ? 'Đóng' : 'Quay lại chỉnh sửa'),
          ),
        ],
      ),
    );
  }

  void _editAndRetry(Map<String, dynamic> item) {
    setState(() {
      _retryOfAnnouncementId = item['id'] as int;
      _title.text = item['title'] as String? ?? '';
      _body.text = item['body'] as String? ?? '';
      _target = item['targetRole'] as String? ?? 'ALL';
      _classIds
        ..clear()
        ..addAll((item['classIds'] as List? ?? const []).whereType<int>());
      if (_classIds.isEmpty) {
        final classNames = item['classNames'] as List? ?? const [];
        for (final schoolClass in _classes) {
          if (classNames.contains(schoolClass['name'])) {
            _classIds.add(schoolClass['id'] as int);
          }
        }
      }
    });
    _message('Đã nạp nội dung bị từ chối. Hãy chỉnh sửa rồi gửi lại.');
  }

  Future<void> _delete(int id) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Xóa thông báo?'),
        content: const Text(
          'Thông báo sẽ bị xóa khỏi hệ thống. Hành động này không thể hoàn tác.',
        ),
        actions: [
          TextButton(
            key: const ValueKey('cancel-delete-announcement'),
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            key: const ValueKey('confirm-delete-announcement'),
            style: FilledButton.styleFrom(backgroundColor: AppColors.danger),
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: const Text('Xóa'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    try {
      await _api.deleteData('/api/announcements/$id', token: widget.token);
      setState(() {
        _sent = _sent.where((item) => item['id'] != id).toList();
        if (_retryOfAnnouncementId == id) _retryOfAnnouncementId = null;
      });
      _message('Đã xóa thông báo.');
    } catch (error) {
      _message(error.toString());
    }
  }

  void _resetDraft() {
    _retryOfAnnouncementId = null;
    _title.clear();
    _body.clear();
    _target = 'ALL';
    _classIds.clear();
  }

  void _message(String text) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: AppColors.background,
    appBar: const OrangeTopBar(title: 'Gửi thông báo lớp'),
    body: _loading
        ? const Center(child: CircularProgressIndicator())
        : RefreshIndicator(
            onRefresh: _load,
            child: ListView(
              padding: const EdgeInsets.all(AppSpacing.lg),
              children: [
                AppCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _retryOfAnnouncementId == null
                            ? 'Tạo thông báo'
                            : 'Chỉnh sửa nội dung bị từ chối',
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        _retryOfAnnouncementId == null
                            ? 'Hệ thống sẽ kiểm tra chính sách trước khi gửi ngay đến người nhận.'
                            : 'Bản bị từ chối vẫn được giữ trong lịch sử.',
                        style: const TextStyle(color: AppColors.muted),
                      ),
                      const SizedBox(height: 14),
                      SegmentedButton<String>(
                        segments: const [
                          ButtonSegment(value: 'ALL', label: Text('Cả hai')),
                          ButtonSegment(
                            value: 'PARENT',
                            label: Text('Phụ huynh'),
                          ),
                          ButtonSegment(
                            value: 'STUDENT',
                            label: Text('Học sinh'),
                          ),
                        ],
                        selected: {_target},
                        onSelectionChanged: _sending
                            ? null
                            : (values) =>
                                  setState(() => _target = values.first),
                      ),
                      const SizedBox(height: 14),
                      const Text(
                        'Lớp được phân công',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 6),
                      Wrap(
                        spacing: 8,
                        children: _classes
                            .map(
                              (schoolClass) => FilterChip(
                                label: Text(
                                  '${schoolClass['name']}${schoolClass['isHomeroom'] == true ? ' · GVCN' : ''}',
                                ),
                                selected: _classIds.contains(schoolClass['id']),
                                onSelected: _sending
                                    ? null
                                    : (selected) => setState(() {
                                        selected
                                            ? _classIds.add(
                                                schoolClass['id'] as int,
                                              )
                                            : _classIds.remove(
                                                schoolClass['id'],
                                              );
                                      }),
                              ),
                            )
                            .toList(),
                      ),
                      if (_classes.isEmpty)
                        const Padding(
                          padding: EdgeInsets.all(8),
                          child: Text(
                            'Không có lớp được phân công trong năm học này.',
                          ),
                        ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _title,
                        enabled: !_sending,
                        maxLength: 500,
                        decoration: const InputDecoration(
                          labelText: 'Tiêu đề',
                          border: OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _body,
                        enabled: !_sending,
                        minLines: 4,
                        maxLines: 8,
                        decoration: const InputDecoration(
                          labelText: 'Nội dung',
                          border: OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 14),
                      AnimatedSwitcher(
                        duration: const Duration(milliseconds: 180),
                        child: _sending
                            ? const Padding(
                                key: ValueKey('announcement-policy-loading'),
                                padding: EdgeInsets.only(bottom: 12),
                                child: Row(
                                  children: [
                                    SizedBox(
                                      width: 20,
                                      height: 20,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2.5,
                                      ),
                                    ),
                                    SizedBox(width: 10),
                                    Expanded(
                                      child: Text(
                                        'Đang kiểm tra nội dung thông báo…',
                                        style: TextStyle(
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              )
                            : const SizedBox.shrink(),
                      ),
                      Row(
                        children: [
                          Expanded(
                            child: FilledButton.icon(
                              key: const ValueKey('send-announcement'),
                              onPressed: _sending ? null : _save,
                              icon: const Icon(Icons.send),
                              label: Text(
                                _sending
                                    ? 'Đang kiểm tra nội dung…'
                                    : _retryOfAnnouncementId == null
                                    ? 'Gửi thông báo'
                                    : 'Gửi lại thông báo',
                              ),
                            ),
                          ),
                          if (_retryOfAnnouncementId != null)
                            TextButton(
                              onPressed: _sending
                                  ? null
                                  : () => setState(_resetDraft),
                              child: const Text('Hủy gửi lại'),
                            ),
                        ],
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 18),
                const Text(
                  'Lịch sử đã gửi',
                  style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 8),
                if (_sent.isEmpty)
                  const AppCard(child: Text('Chưa có thông báo đã gửi.')),
                ..._sent.map(
                  (announcement) => Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: AppCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Expanded(
                                child: Text(
                                  announcement['title'] as String? ?? '',
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ),
                              Chip(
                                backgroundColor:
                                    announcement['deliveryStatus'] ==
                                        'PUBLISHED'
                                    ? AppColors.green.withValues(alpha: .12)
                                    : AppColors.danger.withValues(alpha: .1),
                                label: Text(
                                  _status(
                                    announcement['deliveryStatus'] as String?,
                                  ),
                                ),
                              ),
                            ],
                          ),
                          Text(
                            announcement['body'] as String? ?? '',
                            maxLines: 3,
                            overflow: TextOverflow.ellipsis,
                          ),
                          const SizedBox(height: 6),
                          Text(
                            '${(announcement['classNames'] as List? ?? const []).join(', ')} · ${_audience(announcement['targetRole'])}',
                            style: const TextStyle(color: AppColors.muted),
                          ),
                          if (announcement['systemRejectionMessage'] is String)
                            Padding(
                              padding: const EdgeInsets.only(top: 6),
                              child: Text(
                                announcement['systemRejectionMessage']
                                    as String,
                                style: const TextStyle(color: AppColors.danger),
                              ),
                            ),
                          ..._violationWidgets(announcement['violations']),
                          if (announcement['deliveryStatus'] ==
                              'SYSTEM_REJECTED')
                            Row(
                              mainAxisAlignment: MainAxisAlignment.end,
                              children: [
                                TextButton.icon(
                                  key: ValueKey(
                                    'retry-announcement-${announcement['id']}',
                                  ),
                                  onPressed: () => _editAndRetry(announcement),
                                  icon: const Icon(Icons.edit),
                                  label: const Text('Sửa và gửi lại'),
                                ),
                                TextButton.icon(
                                  key: ValueKey(
                                    'delete-announcement-${announcement['id']}',
                                  ),
                                  onPressed: () =>
                                      _delete(announcement['id'] as int),
                                  icon: const Icon(Icons.delete),
                                  label: const Text('Xóa'),
                                ),
                              ],
                            ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
  );

  List<Widget> _violationWidgets(Object? raw) {
    final violations = (raw as List? ?? const [])
        .whereType<Map<String, dynamic>>()
        .toList();
    if (violations.isEmpty) return const [];
    return [
      const SizedBox(height: 6),
      ...violations.map(
        (violation) => Text(
          '${_fieldLabel(violation['field'])}: “${violation['phrase']}”',
          style: const TextStyle(fontSize: 12, color: AppColors.danger),
        ),
      ),
    ];
  }

  String _status(String? status) =>
      status == 'PUBLISHED' ? 'Gửi thành công' : 'Hệ thống từ chối';

  String _audience(Object? target) => target == 'PARENT'
      ? 'Phụ huynh'
      : target == 'STUDENT'
      ? 'Học sinh'
      : 'Phụ huynh & học sinh';

  static String _fieldLabel(Object? field) =>
      field == 'TITLE' ? 'Tiêu đề' : 'Nội dung';
}
