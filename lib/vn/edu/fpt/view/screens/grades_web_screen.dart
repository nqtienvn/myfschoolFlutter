import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_radius.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/primary_button.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class TeacherStudentGrade {
  TeacherStudentGrade({
    required this.studentName,
    required this.studentCode,
    required this.oralScores,
    required this.fifteenMinScores,
    required this.onePeriodScores,
    required this.semesterScore,
    this.isCommentBased = false,
    this.commentOralScores = const [],
    this.commentFifteenMinScores = const [],
    this.commentOnePeriodScores = const [],
    this.commentSemesterScore = '',
  });

  final String studentName;
  final String studentCode;
  List<double> oralScores;
  List<double> fifteenMinScores;
  List<double> onePeriodScores;
  double semesterScore;

  final bool isCommentBased;
  List<String> commentOralScores;
  List<String> commentFifteenMinScores;
  List<String> commentOnePeriodScores;
  String commentSemesterScore;

  TeacherStudentGrade copyWith({
    List<double>? oralScores,
    List<double>? fifteenMinScores,
    List<double>? onePeriodScores,
    double? semesterScore,
    List<String>? commentOralScores,
    List<String>? commentFifteenMinScores,
    List<String>? commentOnePeriodScores,
    String? commentSemesterScore,
  }) {
    return TeacherStudentGrade(
      studentName: studentName,
      studentCode: studentCode,
      oralScores: oralScores ?? List.from(this.oralScores),
      fifteenMinScores: fifteenMinScores ?? List.from(this.fifteenMinScores),
      onePeriodScores: onePeriodScores ?? List.from(this.onePeriodScores),
      semesterScore: semesterScore ?? this.semesterScore,
      isCommentBased: isCommentBased,
      commentOralScores: commentOralScores ?? List.from(this.commentOralScores),
      commentFifteenMinScores: commentFifteenMinScores ?? List.from(this.commentFifteenMinScores),
      commentOnePeriodScores: commentOnePeriodScores ?? List.from(this.commentOnePeriodScores),
      commentSemesterScore: commentSemesterScore ?? this.commentSemesterScore,
    );
  }

  String get tbm {
    if (isCommentBased) {
      if (commentSemesterScore == 'KĐ') return 'KĐ';
      int passCount = 0;
      int totalCount = 0;
      for (var s in commentOralScores) {
        if (s == 'Đ') passCount++;
        totalCount++;
      }
      for (var s in commentFifteenMinScores) {
        if (s == 'Đ') passCount++;
        totalCount++;
      }
      for (var s in commentOnePeriodScores) {
        if (s == 'Đ') passCount++;
        totalCount++;
      }
      if (commentSemesterScore == 'Đ') passCount++;
      totalCount++;

      if (totalCount == 0) return 'KĐ';
      double ratio = passCount / totalCount;
      return ratio >= 0.65 ? 'Đạt' : 'KĐ';
    } else {
      double sum = 0;
      int count = 0;
      for (var s in oralScores) {
        sum += s;
        count++;
      }
      for (var s in fifteenMinScores) {
        sum += s;
        count++;
      }
      for (var s in onePeriodScores) {
        sum += s * 2;
        count += 2;
      }
      sum += semesterScore * 3;
      count += 3;

      if (count == 0) return '0.0';
      double avg = sum / count;
      return avg.toStringAsFixed(1);
    }
  }
}

class GradesWebScreen extends StatefulWidget {
  const GradesWebScreen({super.key});

  @override
  State<GradesWebScreen> createState() => _GradesWebScreenState();
}

class _GradesWebScreenState extends State<GradesWebScreen> {
  bool _uploaded = false;
  String _selectedClass = '12A1';
  String _selectedSubject = 'Toán học';

  final List<String> _classes = ['12A1', '10B2'];
  final List<String> _subjects = ['Toán học', 'Ngữ văn', 'Vật lí', 'Thể dục'];

  late Map<String, List<TeacherStudentGrade>> _gradeData;

  @override
  void initState() {
    super.initState();
    _initGradeData();
  }

  void _initGradeData() {
    _gradeData = {
      '12A1_Toán học': [
        TeacherStudentGrade(
          studentName: 'Nguyễn Minh An',
          studentCode: 'MFS-1201',
          oralScores: [9.0, 10.0, 10.0, 9.5],
          fifteenMinScores: [10.0],
          onePeriodScores: [9.0],
          semesterScore: 9.5,
        ),
        TeacherStudentGrade(
          studentName: 'Trần Hoàng Nam',
          studentCode: 'MFS-1207',
          oralScores: [8.0, 9.0],
          fifteenMinScores: [8.5],
          onePeriodScores: [8.0],
          semesterScore: 8.5,
        ),
      ],
      '12A1_Ngữ văn': [
        TeacherStudentGrade(
          studentName: 'Nguyễn Minh An',
          studentCode: 'MFS-1201',
          oralScores: [10.0, 10.0, 8.0, 10.0],
          fifteenMinScores: [8.0],
          onePeriodScores: [7.5],
          semesterScore: 8.5,
        ),
        TeacherStudentGrade(
          studentName: 'Trần Hoàng Nam',
          studentCode: 'MFS-1207',
          oralScores: [7.0, 8.0],
          fifteenMinScores: [7.5],
          onePeriodScores: [8.0],
          semesterScore: 8.0,
        ),
      ],
      '12A1_Vật lí': [
        TeacherStudentGrade(
          studentName: 'Nguyễn Minh An',
          studentCode: 'MFS-1201',
          oralScores: [9.0, 9.0],
          fifteenMinScores: [8.8],
          onePeriodScores: [9.5],
          semesterScore: 9.2,
        ),
        TeacherStudentGrade(
          studentName: 'Trần Hoàng Nam',
          studentCode: 'MFS-1207',
          oralScores: [8.0, 8.0],
          fifteenMinScores: [8.0],
          onePeriodScores: [8.5],
          semesterScore: 8.8,
        ),
      ],
      '12A1_Thể dục': [
        TeacherStudentGrade(
          studentName: 'Nguyễn Minh An',
          studentCode: 'MFS-1201',
          oralScores: [],
          fifteenMinScores: [],
          onePeriodScores: [],
          semesterScore: 0.0,
          isCommentBased: true,
          commentOralScores: ['Đ', 'Đ', 'Đ'],
          commentFifteenMinScores: ['Đ'],
          commentOnePeriodScores: ['Đ'],
          commentSemesterScore: 'Đ',
        ),
        TeacherStudentGrade(
          studentName: 'Trần Hoàng Nam',
          studentCode: 'MFS-1207',
          oralScores: [],
          fifteenMinScores: [],
          onePeriodScores: [],
          semesterScore: 0.0,
          isCommentBased: true,
          commentOralScores: ['Đ', 'KĐ', 'Đ'],
          commentFifteenMinScores: ['Đ'],
          commentOnePeriodScores: ['Đ'],
          commentSemesterScore: 'Đ',
        ),
      ],
      '10B2_Toán học': [
        TeacherStudentGrade(
          studentName: 'Nguyễn Minh Bảo',
          studentCode: 'MFS-1007',
          oralScores: [8.0, 8.0],
          fifteenMinScores: [7.5],
          onePeriodScores: [8.0],
          semesterScore: 8.0,
        ),
      ],
      '10B2_Ngữ văn': [
        TeacherStudentGrade(
          studentName: 'Nguyễn Minh Bảo',
          studentCode: 'MFS-1007',
          oralScores: [7.8, 8.0],
          fifteenMinScores: [7.0],
          onePeriodScores: [8.0],
          semesterScore: 8.2,
        ),
      ],
      '10B2_Vật lí': [
        TeacherStudentGrade(
          studentName: 'Nguyễn Minh Bảo',
          studentCode: 'MFS-1007',
          oralScores: [8.5, 9.0],
          fifteenMinScores: [8.0],
          onePeriodScores: [8.5],
          semesterScore: 8.5,
        ),
      ],
      '10B2_Thể dục': [
        TeacherStudentGrade(
          studentName: 'Nguyễn Minh Bảo',
          studentCode: 'MFS-1007',
          oralScores: [],
          fifteenMinScores: [],
          onePeriodScores: [],
          semesterScore: 0.0,
          isCommentBased: true,
          commentOralScores: ['Đ', 'Đ'],
          commentFifteenMinScores: ['Đ'],
          commentOnePeriodScores: ['Đ'],
          commentSemesterScore: 'Đ',
        ),
      ],
    };
  }

  void _showEditStudentGradeSheet(TeacherStudentGrade sGrade, int listIndex, String key) {
    if (sGrade.isCommentBased) {
      _showCommentEditSheet(sGrade, listIndex, key);
    } else {
      _showNumericalEditSheet(sGrade, listIndex, key);
    }
  }

  void _showNumericalEditSheet(TeacherStudentGrade sGrade, int listIndex, String key) {
    final oralController = TextEditingController(text: sGrade.oralScores.map((s) => s.toString()).join(', '));
    final fifteenController = TextEditingController(text: sGrade.fifteenMinScores.map((s) => s.toString()).join(', '));
    final onePeriodController = TextEditingController(text: sGrade.onePeriodScores.map((s) => s.toString()).join(', '));
    final semesterController = TextEditingController(text: sGrade.semesterScore.toString());
    final formKey = GlobalKey<FormState>();

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (context) {
        return Padding(
          padding: EdgeInsets.only(
            bottom: MediaQuery.of(context).viewInsets.bottom,
            top: 24,
            left: 24,
            right: 24,
          ),
          child: Form(
            key: formKey,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'Nhập điểm: ${sGrade.studentName}',
                        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.ink),
                      ),
                      IconButton(
                        icon: const Icon(Icons.close),
                        onPressed: () => Navigator.pop(context),
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.md),
                  _buildScoreField(
                    label: 'Điểm miệng',
                    controller: oralController,
                    hint: 'Ví dụ: 8, 9, 10 (cách nhau bởi dấu phẩy)',
                  ),
                  const SizedBox(height: AppSpacing.md),
                  _buildScoreField(
                    label: 'Điểm 15 phút',
                    controller: fifteenController,
                    hint: 'Ví dụ: 8.5, 9',
                  ),
                  const SizedBox(height: AppSpacing.md),
                  _buildScoreField(
                    label: 'Điểm 1 tiết',
                    controller: onePeriodController,
                    hint: 'Ví dụ: 8, 7.5',
                  ),
                  const SizedBox(height: AppSpacing.md),
                  TextFormField(
                    controller: semesterController,
                    decoration: const InputDecoration(
                      labelText: 'Điểm thi học kỳ',
                      labelStyle: TextStyle(fontSize: 13, color: AppColors.muted),
                      border: OutlineInputBorder(),
                      contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    ),
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    validator: (val) {
                      if (val == null || val.trim().isEmpty) return 'Không được để trống';
                      final numVal = double.tryParse(val.trim());
                      if (numVal == null || numVal < 0 || numVal > 10) return 'Điểm hợp lệ từ 0 đến 10';
                      return null;
                    },
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton(
                          onPressed: () => Navigator.pop(context),
                          child: const Text('Hủy', style: TextStyle(color: AppColors.muted)),
                        ),
                      ),
                      const SizedBox(width: AppSpacing.md),
                      Expanded(
                        child: ElevatedButton(
                          onPressed: () {
                            if (formKey.currentState!.validate()) {
                              final List<double> orals = oralController.text
                                  .split(',')
                                  .map((s) => double.tryParse(s.trim()))
                                  .whereType<double>()
                                  .toList();
                              final List<double> fifteens = fifteenController.text
                                  .split(',')
                                  .map((s) => double.tryParse(s.trim()))
                                  .whereType<double>()
                                  .toList();
                              final List<double> onePeriods = onePeriodController.text
                                  .split(',')
                                  .map((s) => double.tryParse(s.trim()))
                                  .whereType<double>()
                                  .toList();
                              final double semester = double.parse(semesterController.text.trim());

                              setState(() {
                                _gradeData[key]![listIndex] = sGrade.copyWith(
                                  oralScores: orals,
                                  fifteenMinScores: fifteens,
                                  onePeriodScores: onePeriods,
                                  semesterScore: semester,
                                );
                              });
                              Navigator.pop(context);
                            }
                          },
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppColors.fptOrange,
                          ),
                          child: const Text('Lưu điểm', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  void _showCommentEditSheet(TeacherStudentGrade sGrade, int listIndex, String key) {
    List<String> tempOral = List.from(sGrade.commentOralScores);
    List<String> tempFifteen = List.from(sGrade.commentFifteenMinScores);
    List<String> tempOnePeriod = List.from(sGrade.commentOnePeriodScores);
    String tempSemester = sGrade.commentSemesterScore;

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setSheetState) {
            return Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'Nhập nhận xét: ${sGrade.studentName}',
                        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.ink),
                      ),
                      IconButton(
                        icon: const Icon(Icons.close),
                        onPressed: () => Navigator.pop(context),
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.md),
                  const Text('Điểm miệng (ĐGTX)', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.ink)),
                  const SizedBox(height: AppSpacing.xs),
                  Row(
                    children: List.generate(tempOral.length, (idx) {
                      return Padding(
                        padding: const EdgeInsets.only(right: 8.0),
                        child: ChoiceChip(
                          label: Text('Cột ${idx + 1}: ${tempOral[idx]}'),
                          selected: tempOral[idx] == 'Đ',
                          selectedColor: AppColors.greenSoft,
                          labelStyle: TextStyle(
                            color: tempOral[idx] == 'Đ' ? AppColors.green : AppColors.muted,
                            fontWeight: FontWeight.bold,
                          ),
                          onSelected: (selected) {
                            setSheetState(() {
                              tempOral[idx] = selected ? 'Đ' : 'KĐ';
                            });
                          },
                        ),
                      );
                    }),
                  ),
                  const SizedBox(height: AppSpacing.md),
                  const Text('Điểm 15 phút (ĐGTX)', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.ink)),
                  const SizedBox(height: AppSpacing.xs),
                  Row(
                    children: List.generate(tempFifteen.length, (idx) {
                      return Padding(
                        padding: const EdgeInsets.only(right: 8.0),
                        child: ChoiceChip(
                          label: Text('Cột ${idx + 1}: ${tempFifteen[idx]}'),
                          selected: tempFifteen[idx] == 'Đ',
                          selectedColor: AppColors.greenSoft,
                          labelStyle: TextStyle(
                            color: tempFifteen[idx] == 'Đ' ? AppColors.green : AppColors.muted,
                            fontWeight: FontWeight.bold,
                          ),
                          onSelected: (selected) {
                            setSheetState(() {
                              tempFifteen[idx] = selected ? 'Đ' : 'KĐ';
                            });
                          },
                        ),
                      );
                    }),
                  ),
                  const SizedBox(height: AppSpacing.md),
                  const Text('Điểm 1 tiết (ĐGGK)', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.ink)),
                  const SizedBox(height: AppSpacing.xs),
                  Row(
                    children: List.generate(tempOnePeriod.length, (idx) {
                      return Padding(
                        padding: const EdgeInsets.only(right: 8.0),
                        child: ChoiceChip(
                          label: Text('Cột ${idx + 1}: ${tempOnePeriod[idx]}'),
                          selected: tempOnePeriod[idx] == 'Đ',
                          selectedColor: AppColors.greenSoft,
                          labelStyle: TextStyle(
                            color: tempOnePeriod[idx] == 'Đ' ? AppColors.green : AppColors.muted,
                            fontWeight: FontWeight.bold,
                          ),
                          onSelected: (selected) {
                            setSheetState(() {
                              tempOnePeriod[idx] = selected ? 'Đ' : 'KĐ';
                            });
                          },
                        ),
                      );
                    }),
                  ),
                  const SizedBox(height: AppSpacing.md),
                  const Text('Thi Học kỳ (ĐGCK)', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.ink)),
                  const SizedBox(height: AppSpacing.xs),
                  ChoiceChip(
                    label: Text('Học kỳ: $tempSemester'),
                    selected: tempSemester == 'Đ',
                    selectedColor: AppColors.greenSoft,
                    labelStyle: TextStyle(
                      color: tempSemester == 'Đ' ? AppColors.green : AppColors.muted,
                      fontWeight: FontWeight.bold,
                    ),
                    onSelected: (selected) {
                      setSheetState(() {
                        tempSemester = selected ? 'Đ' : 'KĐ';
                      });
                    },
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton(
                          onPressed: () => Navigator.pop(context),
                          child: const Text('Hủy', style: TextStyle(color: AppColors.muted)),
                        ),
                      ),
                      const SizedBox(width: AppSpacing.md),
                      Expanded(
                        child: ElevatedButton(
                          onPressed: () {
                            setState(() {
                              _gradeData[key]![listIndex] = sGrade.copyWith(
                                commentOralScores: tempOral,
                                commentFifteenMinScores: tempFifteen,
                                commentOnePeriodScores: tempOnePeriod,
                                commentSemesterScore: tempSemester,
                              );
                            });
                            Navigator.pop(context);
                          },
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppColors.fptOrange,
                          ),
                          child: const Text('Lưu nhận xét', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                ],
              ),
            );
          },
        );
      },
    );
  }

  Widget _buildScoreField({required String label, required TextEditingController controller, required String hint}) {
    return TextFormField(
      controller: controller,
      decoration: InputDecoration(
        labelText: label,
        labelStyle: const TextStyle(fontSize: 13, color: AppColors.muted),
        hintText: hint,
        hintStyle: const TextStyle(fontSize: 12, color: Colors.grey),
        border: const OutlineInputBorder(),
        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      ),
      validator: (val) {
        if (val == null || val.trim().isEmpty) return null;
        final parts = val.split(',');
        for (var part in parts) {
          if (part.trim().isEmpty) continue;
          final numVal = double.tryParse(part.trim());
          if (numVal == null || numVal < 0 || numVal > 10) {
            return 'Danh sách điểm chứa giá trị không hợp lệ ($part)';
          }
        }
        return null;
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final dataKey = '${_selectedClass}_$_selectedSubject';
    final studentsList = _gradeData[dataKey] ?? [];

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Nhập & Upload điểm (Giáo viên)'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            // Filter card
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Bộ lọc tìm kiếm lớp học',
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppColors.ink),
                  ),
                  const SizedBox(height: AppSpacing.md),
                  Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('Lớp học', style: TextStyle(fontSize: 11, color: AppColors.muted)),
                            DropdownButton<String>(
                              isExpanded: true,
                              value: _selectedClass,
                              items: _classes.map((c) => DropdownMenuItem(value: c, child: Text(c, style: const TextStyle(fontSize: 13)))).toList(),
                              onChanged: (val) {
                                if (val != null) setState(() => _selectedClass = val);
                              },
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: AppSpacing.md),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('Môn học', style: TextStyle(fontSize: 11, color: AppColors.muted)),
                            DropdownButton<String>(
                              isExpanded: true,
                              value: _selectedSubject,
                              items: _subjects.map((s) => DropdownMenuItem(value: s, child: Text(s, style: const TextStyle(fontSize: 13)))).toList(),
                              onChanged: (val) {
                                if (val != null) setState(() => _selectedSubject = val);
                              },
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.md),

            // Grade entry card
            Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppColors.line),
              ),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
                      color: AppColors.fptOrange,
                      child: Text(
                        'Danh sách điểm lớp $_selectedClass - Môn $_selectedSubject',
                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
                      ),
                    ),
                    SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      child: DataTable(
                        columnSpacing: 16,
                        horizontalMargin: 12,
                        columns: const [
                          DataColumn(label: Text('Học sinh', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppColors.ink))),
                          DataColumn(label: Text('Mã HS', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppColors.ink))),
                          DataColumn(label: Text('Điểm miệng', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppColors.ink))),
                          DataColumn(label: Text('Điểm 15p', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppColors.ink))),
                          DataColumn(label: Text('Điểm 1 tiết', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppColors.ink))),
                          DataColumn(label: Text('Học kỳ', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppColors.ink))),
                          DataColumn(label: Text('TBM', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppColors.ink))),
                        ],
                        rows: List.generate(studentsList.length, (idx) {
                          final sGrade = studentsList[idx];
                          final isGraded = !sGrade.isCommentBased;

                          final oralText = isGraded
                              ? (sGrade.oralScores.isEmpty ? '-' : sGrade.oralScores.map((s) => s.toString().replaceAll('.0', '')).join('  '))
                              : (sGrade.commentOralScores.isEmpty ? '-' : sGrade.commentOralScores.join('  '));

                          final fifteenText = isGraded
                              ? (sGrade.fifteenMinScores.isEmpty ? '-' : sGrade.fifteenMinScores.map((s) => s.toString().replaceAll('.0', '')).join('  '))
                              : (sGrade.commentFifteenMinScores.isEmpty ? '-' : sGrade.commentFifteenMinScores.join('  '));

                          final onePeriodText = isGraded
                              ? (sGrade.onePeriodScores.isEmpty ? '-' : sGrade.onePeriodScores.map((s) => s.toString().replaceAll('.0', '')).join('  '))
                              : (sGrade.commentOnePeriodScores.isEmpty ? '-' : sGrade.commentOnePeriodScores.join('  '));

                          final semesterText = isGraded
                              ? (sGrade.semesterScore == 0.0 ? '-' : sGrade.semesterScore.toString().replaceAll('.0', ''))
                              : (sGrade.commentSemesterScore.isEmpty ? '-' : sGrade.commentSemesterScore);

                          final tbmVal = sGrade.tbm;
                          final isHigh = tbmVal == 'Đạt' || tbmVal == 'Đ' || (double.tryParse(tbmVal) ?? 0) >= 8.0;

                          return DataRow(
                            cells: [
                              DataCell(
                                Text(sGrade.studentName, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12.5, color: AppColors.ink)),
                              ),
                              DataCell(
                                Text(sGrade.studentCode, style: const TextStyle(fontSize: 12, color: AppColors.muted)),
                              ),
                              DataCell(
                                InkWell(
                                  onTap: () => _showEditStudentGradeSheet(sGrade, idx, dataKey),
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                                    decoration: BoxDecoration(border: Border(bottom: BorderSide(style: BorderStyle.solid, width: 1.0, color: Colors.grey.shade400))),
                                    child: Text(oralText, style: const TextStyle(fontSize: 12, color: AppColors.muted)),
                                  ),
                                ),
                              ),
                              DataCell(
                                InkWell(
                                  onTap: () => _showEditStudentGradeSheet(sGrade, idx, dataKey),
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                                    decoration: BoxDecoration(border: Border(bottom: BorderSide(style: BorderStyle.solid, width: 1.0, color: Colors.grey.shade400))),
                                    child: Text(fifteenText, style: const TextStyle(fontSize: 12, color: AppColors.muted)),
                                  ),
                                ),
                              ),
                              DataCell(
                                InkWell(
                                  onTap: () => _showEditStudentGradeSheet(sGrade, idx, dataKey),
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                                    decoration: BoxDecoration(border: Border(bottom: BorderSide(style: BorderStyle.solid, width: 1.0, color: Colors.grey.shade400))),
                                    child: Text(onePeriodText, style: const TextStyle(fontSize: 12, color: AppColors.muted)),
                                  ),
                                ),
                              ),
                              DataCell(
                                InkWell(
                                  onTap: () => _showEditStudentGradeSheet(sGrade, idx, dataKey),
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                                    decoration: BoxDecoration(border: Border(bottom: BorderSide(style: BorderStyle.solid, width: 1.0, color: Colors.grey.shade400))),
                                    child: Text(semesterText, style: const TextStyle(fontSize: 12, color: AppColors.muted)),
                                  ),
                                ),
                              ),
                              DataCell(
                                Text(
                                  tbmVal,
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 12.5,
                                    color: isHigh ? AppColors.green : AppColors.fptOrange,
                                  ),
                                ),
                              ),
                            ],
                          );
                        }),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.lg),

            // Upload section
            const SectionHeader(title: 'Nhập điểm nhanh qua biểu mẫu Excel'),
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Tải biểu mẫu nhập điểm và điền thông tin điểm của lớp học. Hệ thống tự động phân tích và đồng bộ hóa tức thì.',
                    style: TextStyle(fontSize: 12.5, color: AppColors.muted, height: 1.35),
                  ),
                  const SizedBox(height: AppSpacing.md),
                  OutlinedButton.icon(
                    onPressed: () {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text('Đang tải biểu mẫu Excel cho lớp $_selectedClass môn $_selectedSubject...'),
                          behavior: SnackBarBehavior.floating,
                        ),
                      );
                    },
                    icon: const Icon(Icons.download, color: AppColors.fptOrange, size: 18),
                    label: const Text('Tải file Excel mẫu', style: TextStyle(color: AppColors.fptOrange, fontWeight: FontWeight.bold)),
                    style: OutlinedButton.styleFrom(
                      side: const BorderSide(color: AppColors.fptOrange),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppRadius.md)),
                    ),
                  ),
                  const Divider(height: AppSpacing.xl),
                  const Text(
                    'Tải lên file Excel kết quả:',
                    style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.ink),
                  ),
                  const SizedBox(height: AppSpacing.sm),
                  InkWell(
                    onTap: () {
                      setState(() {
                        _uploaded = !_uploaded;
                        if (_uploaded) {
                          // Simulate dynamic data upload changes
                          if (_selectedClass == '12A1' && _selectedSubject == 'Toán học') {
                            _gradeData['12A1_Toán học'] = [
                              TeacherStudentGrade(
                                studentName: 'Nguyễn Minh An',
                                studentCode: 'MFS-1201',
                                oralScores: [9.5, 10.0, 10.0, 10.0],
                                fifteenMinScores: [10.0],
                                onePeriodScores: [9.5],
                                semesterScore: 9.8,
                              ),
                              TeacherStudentGrade(
                                studentName: 'Trần Hoàng Nam',
                                studentCode: 'MFS-1207',
                                oralScores: [9.0, 9.0],
                                fifteenMinScores: [9.0],
                                onePeriodScores: [8.5],
                                semesterScore: 9.0,
                              ),
                            ];
                          }
                        } else {
                          _initGradeData();
                        }
                      });
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text(_uploaded ? 'Đã tải lên file kết quả và cập nhật bảng điểm!' : 'Đã xóa file.'),
                          behavior: SnackBarBehavior.floating,
                        ),
                      );
                    },
                    child: Container(
                      width: double.infinity,
                      padding: const EdgeInsets.symmetric(vertical: AppSpacing.xl),
                      decoration: BoxDecoration(
                        border: Border.all(color: _uploaded ? AppColors.green : AppColors.line, style: BorderStyle.solid),
                        borderRadius: BorderRadius.circular(AppRadius.md),
                        color: _uploaded ? AppColors.greenSoft : AppColors.background,
                      ),
                      child: Column(
                        children: [
                          Icon(
                            _uploaded ? Icons.cloud_done : Icons.cloud_upload_outlined,
                            size: 32,
                            color: _uploaded ? AppColors.green : AppColors.muted,
                          ),
                          const SizedBox(height: AppSpacing.sm),
                          Text(
                            _uploaded ? 'MFS_${_selectedClass}_${_selectedSubject}_Graded.xlsx' : 'Nhấp để chọn file kết quả từ thiết bị',
                            style: TextStyle(
                              fontSize: 12.5,
                              color: _uploaded ? AppColors.green : AppColors.muted,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  if (_uploaded) ...[
                    const SizedBox(height: AppSpacing.lg),
                    PrimaryButton(
                      label: 'Nhập điểm vào hệ thống',
                      icon: Icons.check,
                      onPressed: () {
                        Navigator.of(context).pop();
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text('Đã hoàn tất nhập điểm của lớp $_selectedClass vào học bạ!'),
                            behavior: SnackBarBehavior.floating,
                          ),
                        );
                      },
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
