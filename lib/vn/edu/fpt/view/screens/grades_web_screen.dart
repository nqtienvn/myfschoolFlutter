import 'package:flutter/material.dart';
import '../../src/models/gradebook.dart';

class GradesWebScreen extends StatefulWidget {
  const GradesWebScreen({super.key});
  @override
  State<GradesWebScreen> createState() => _GradesWebScreenState();
}

class _GradesWebScreenState extends State<GradesWebScreen> {
  final columns = const [
    GradeColumn(
      id: 1,
      code: 'TX_1',
      name: 'Thường xuyên 1',
      weight: 1,
      entryRole: GradeEntryRole.subjectTeacher,
    ),
    GradeColumn(
      id: 2,
      code: 'TX_2',
      name: 'Thường xuyên 2',
      weight: 1,
      entryRole: GradeEntryRole.subjectTeacher,
    ),
    GradeColumn(
      id: 3,
      code: 'GK_1',
      name: 'Giữa kỳ',
      weight: 2,
      entryRole: GradeEntryRole.admin,
    ),
    GradeColumn(
      id: 4,
      code: 'CK_1',
      name: 'Cuối kỳ',
      weight: 3,
      entryRole: GradeEntryRole.admin,
    ),
  ];
  final students = ['Nguyễn Minh Anh', 'Trần Gia Bảo', 'Lê Hoàng Nam'];
  final values = <String, Map<int, double?>>{};
  String selectedColumn = 'TX_1';
  @override
  void initState() {
    super.initState();
    for (final student in students) {
      values[student] = {1: null, 2: null, 3: null, 4: null};
    }
  }

  @override
  Widget build(BuildContext context) {
    final column = columns.firstWhere((c) => c.code == selectedColumn);
    return Scaffold(
      appBar: AppBar(title: const Text('Nhập điểm môn học')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          DropdownButtonFormField<String>(
            initialValue: selectedColumn,
            decoration: const InputDecoration(
              labelText: 'Đầu điểm theo cấu hình năm học',
            ),
            items: columns
                .map(
                  (c) => DropdownMenuItem(
                    value: c.code,
                    child: Text(
                      '${c.name} · hệ số ${c.weight}${c.teacherCanEdit ? '' : ' · Nhà trường nhập'}',
                    ),
                  ),
                )
                .toList(),
            onChanged: (v) => setState(() => selectedColumn = v!),
          ),
          const SizedBox(height: 12),
          if (!column.teacherCanEdit)
            const Card(
              color: Color(0xfffff5d6),
              child: Padding(
                padding: EdgeInsets.all(12),
                child: Text(
                  'Đầu điểm này do admin/nhà trường nhập. Giáo viên chỉ được xem.',
                ),
              ),
            ),
          ...students.map(
            (student) => Card(
              child: ListTile(
                title: Text(student),
                trailing: SizedBox(
                  width: 90,
                  child: TextFormField(
                    enabled: column.teacherCanEdit,
                    initialValue: values[student]![column.id]?.toString(),
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    decoration: const InputDecoration(hintText: '0–10'),
                    onChanged: (v) =>
                        values[student]![column.id] = double.tryParse(v),
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(height: 12),
          FilledButton.icon(
            onPressed: column.teacherCanEdit
                ? () => ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Đã lưu nháp đầu điểm.')),
                  )
                : null,
            icon: const Icon(Icons.save),
            label: const Text('Lưu nháp'),
          ),
        ],
      ),
    );
  }
}
