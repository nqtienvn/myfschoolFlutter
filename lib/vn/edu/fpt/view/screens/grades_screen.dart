import 'package:flutter/material.dart';
import '../../src/models/gradebook.dart';

class GradesScreen extends StatefulWidget {
  final Object? student;
  const GradesScreen({super.key, this.student});
  @override
  State<GradesScreen> createState() => _GradesScreenState();
}

class _GradesScreenState extends State<GradesScreen> {
  String semester = 'Học kỳ 1';
  bool simulation = false;
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
  late final rows = [
    SubjectTranscript(
      subjectName: 'Toán',
      columns: columns,
      scores: {1: 8, 2: 9, 3: 7.5, 4: 8.5},
      average: 8.2,
    ),
    SubjectTranscript(
      subjectName: 'Ngữ văn',
      columns: columns,
      scores: {1: 7, 2: 8, 3: 8, 4: null},
    ),
    SubjectTranscript(
      subjectName: 'Tiếng Anh',
      columns: columns,
      scores: {1: 9, 2: 8.5, 3: 9, 4: 9},
      average: 8.9,
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.student == null ? 'Bảng điểm' : 'Bảng điểm học sinh',
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Row(
            children: [
              Expanded(
                child: DropdownButtonFormField<String>(
                  initialValue: semester,
                  items: ['Học kỳ 1', 'Học kỳ 2', 'Cả năm']
                      .map((e) => DropdownMenuItem(value: e, child: Text(e)))
                      .toList(),
                  onChanged: (v) => setState(() => semester = v!),
                ),
              ),
              const SizedBox(width: 12),
              FilterChip(
                label: const Text('Mô phỏng'),
                selected: simulation,
                onSelected: (v) => setState(() => simulation = v),
              ),
            ],
          ),
          if (simulation)
            const Padding(
              padding: EdgeInsets.only(top: 12),
              child: Card(
                color: Color(0xfffff5d6),
                child: Padding(
                  padding: EdgeInsets.all(12),
                  child: Text(
                    'Điểm mô phỏng chỉ lưu trên thiết bị, không thay đổi kết quả chính thức.',
                  ),
                ),
              ),
            ),
          const SizedBox(height: 16),
          ...rows.map(
            (row) => Card(
              child: ExpansionTile(
                title: Text(row.subjectName),
                subtitle: Text(
                  row.complete ? 'Đã đủ đầu điểm' : 'Chưa đủ đầu điểm',
                ),
                trailing: Text(
                  row.average?.toStringAsFixed(1) ?? '—',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                children: [
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: DataTable(
                      columns: const [
                        DataColumn(label: Text('Đầu điểm')),
                        DataColumn(label: Text('Hệ số')),
                        DataColumn(label: Text('Điểm')),
                      ],
                      rows: row.columns
                          .map(
                            (column) => DataRow(
                              cells: [
                                DataCell(Text(column.name)),
                                DataCell(Text('${column.weight}')),
                                DataCell(
                                  simulation
                                      ? SizedBox(
                                          width: 72,
                                          child: TextFormField(
                                            initialValue: row.scores[column.id]
                                                ?.toString(),
                                            keyboardType: TextInputType.number,
                                          ),
                                        )
                                      : Text(
                                          row.scores[column.id]
                                                  ?.toStringAsFixed(1) ??
                                              'Chưa có',
                                        ),
                                ),
                              ],
                            ),
                          )
                          .toList(),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
