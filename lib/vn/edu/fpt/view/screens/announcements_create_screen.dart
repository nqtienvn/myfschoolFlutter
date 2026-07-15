import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class AnnouncementsCreateScreen extends StatefulWidget {
  const AnnouncementsCreateScreen({super.key, required this.token});
  final String token;
  @override
  State<AnnouncementsCreateScreen> createState() =>
      _AnnouncementsCreateScreenState();
}

class _AnnouncementsCreateScreenState extends State<AnnouncementsCreateScreen> {
  final _title = TextEditingController();
  final _body = TextEditingController();
  final _api = BackendApiClient();
  List<Map<String, dynamic>> _classes = const [], _sent = const [];
  final Set<int> _classIds = {};
  String _target = 'ALL';
  bool _requiresReply = false;
  int? _editingId;
  bool _loading = true, _sending = false;
  int? _yearId;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final id = AcademicPeriodScope.maybeOf(context)?.selected?.academicYearId;
    if (id != null && id != _yearId) {
      _yearId = id;
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
        _api.getData('/api/announcements/mine', token: widget.token),
      ]);
      if (!mounted) return;
      setState(() {
        _classes = (results[0] as List? ?? const [])
            .whereType<Map<String, dynamic>>()
            .toList();
        _sent = (results[1] as List? ?? const [])
            .whereType<Map<String, dynamic>>()
            .where((a) => a['academicYearId'] == _yearId)
            .toList();
      });
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
      final payload = {
        'title': _title.text.trim(),
        'body': _body.text.trim(),
        'targetRole': _target,
        'requiresReply': _requiresReply,
        'academicYearId': _yearId,
        'classIds': _classIds.toList(),
      };
      if (_editingId == null) {
        await _api.postData(
          '/api/announcements',
          token: widget.token,
          body: payload,
        );
      } else {
        await _api.putData(
          '/api/announcements/$_editingId',
          token: widget.token,
          body: payload,
        );
      }
      _clear();
      await _load();
      _message('Đã gửi thông báo tới admin để phê duyệt.');
    } catch (e) {
      _message(e.toString());
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  void _edit(Map<String, dynamic> item) {
    setState(() {
      _editingId = item['id'] as int;
      _title.text = item['title'] as String? ?? '';
      _body.text = item['body'] as String? ?? '';
      _target = item['targetRole'] as String? ?? 'ALL';
      _requiresReply = item['requiresReply'] == true;
      _classIds.clear();
      for (final c in _classes) {
        if ((item['classNames'] as List? ?? const []).contains(c['name'])) {
          _classIds.add(c['id'] as int);
        }
      }
    });
  }

  Future<void> _delete(int id) async {
    await _api.deleteData('/api/announcements/$id', token: widget.token);
    await _load();
  }

  void _clear() {
    setState(() {
      _editingId = null;
      _title.clear();
      _body.clear();
      _target = 'ALL';
      _requiresReply = false;
      _classIds.clear();
    });
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
                        _editingId == null ? 'Tạo thông báo' : 'Sửa thông báo',
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 12),
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
                        onSelectionChanged: (v) =>
                            setState(() => _target = v.first),
                      ),
                      const SizedBox(height: 14),
                      SwitchListTile(
                        contentPadding: EdgeInsets.zero,
                        title: const Text('Yêu cầu xác nhận hoặc phản hồi'),
                        subtitle: const Text(
                          'Phụ huynh/học sinh sẽ thấy hành động bắt buộc.',
                        ),
                        value: _requiresReply,
                        onChanged: (value) =>
                            setState(() => _requiresReply = value),
                      ),
                      const SizedBox(height: 6),
                      const Text(
                        'Lớp được phân công',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 6),
                      Wrap(
                        spacing: 8,
                        children: _classes
                            .map(
                              (c) => FilterChip(
                                label: Text(
                                  '${c['name']}${c['isHomeroom'] == true ? ' · GVCN' : ''}',
                                ),
                                selected: _classIds.contains(c['id']),
                                onSelected: (v) => setState(() {
                                  v
                                      ? _classIds.add(c['id'] as int)
                                      : _classIds.remove(c['id']);
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
                        decoration: const InputDecoration(
                          labelText: 'Tiêu đề',
                          border: OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _body,
                        minLines: 4,
                        maxLines: 8,
                        decoration: const InputDecoration(
                          labelText: 'Nội dung',
                          border: OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 14),
                      Row(
                        children: [
                          Expanded(
                            child: FilledButton.icon(
                              onPressed: _sending ? null : _save,
                              icon: const Icon(Icons.send),
                              label: Text(
                                _sending
                                    ? 'Đang gửi...'
                                    : _editingId == null
                                    ? 'Gửi duyệt'
                                    : 'Gửi duyệt lại',
                              ),
                            ),
                          ),
                          if (_editingId != null)
                            TextButton(
                              onPressed: _clear,
                              child: const Text('Hủy sửa'),
                            ),
                        ],
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 18),
                const Text(
                  'Thông báo đã gửi',
                  style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 8),
                ..._sent.map(
                  (a) => Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: AppCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Expanded(
                                child: Text(
                                  a['title'] ?? '',
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ),
                              Chip(
                                label: Text(
                                  _status(a['approvalStatus'] as String?),
                                ),
                              ),
                            ],
                          ),
                          Text(
                            a['body'] ?? '',
                            maxLines: 3,
                            overflow: TextOverflow.ellipsis,
                          ),
                          const SizedBox(height: 6),
                          Text(
                            '${(a['classNames'] as List? ?? const []).join(', ')} · ${_audience(a['targetRole'])}',
                            style: const TextStyle(color: AppColors.muted),
                          ),
                          if (a['rejectionReason'] != null)
                            Text(
                              'Lý do từ chối: ${a['rejectionReason']}',
                              style: const TextStyle(color: AppColors.danger),
                            ),
                          if (a['approvalStatus'] != 'APPROVED')
                            Row(
                              mainAxisAlignment: MainAxisAlignment.end,
                              children: [
                                TextButton.icon(
                                  onPressed: () => _edit(a),
                                  icon: const Icon(Icons.edit),
                                  label: const Text('Sửa'),
                                ),
                                TextButton.icon(
                                  onPressed: () => _delete(a['id'] as int),
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
  String _status(String? s) => s == 'APPROVED'
      ? 'Đã duyệt'
      : s == 'REJECTED'
      ? 'Từ chối'
      : 'Chờ duyệt';
  String _audience(Object? s) => s == 'PARENT'
      ? 'Phụ huynh'
      : s == 'STUDENT'
      ? 'Học sinh'
      : 'Phụ huynh & học sinh';
}
