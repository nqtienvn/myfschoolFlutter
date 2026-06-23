import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class SubjectGrade {
  SubjectGrade({
    required this.subject,
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

  final String subject;
  List<double> oralScores;
  List<double> fifteenMinScores;
  List<double> onePeriodScores;
  double semesterScore;

  final bool isCommentBased;
  List<String> commentOralScores;
  List<String> commentFifteenMinScores;
  List<String> commentOnePeriodScores;
  String commentSemesterScore;

  SubjectGrade copyWith({
    List<double>? oralScores,
    List<double>? fifteenMinScores,
    List<double>? onePeriodScores,
    double? semesterScore,
    List<String>? commentOralScores,
    List<String>? commentFifteenMinScores,
    List<String>? commentOnePeriodScores,
    String? commentSemesterScore,
  }) {
    return SubjectGrade(
      subject: subject,
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

  double? get tbmDouble {
    if (isCommentBased) return null;
    return double.tryParse(tbm);
  }
}

class GradesScreen extends StatefulWidget {
  const GradesScreen({super.key});

  @override
  State<GradesScreen> createState() => _GradesScreenState();
}

class _GradesScreenState extends State<GradesScreen> {
  bool _isSimulationMode = false;
  String _conduct = 'Tốt';
  final int _rank = 4;
  late List<SubjectGrade> _grades;

  @override
  void initState() {
    super.initState();
    _resetGrades();
  }

  void _resetGrades() {
    _grades = [
      SubjectGrade(
        subject: 'Toán học',
        oralScores: [9.0, 10.0, 10.0, 9.5],
        fifteenMinScores: [10.0],
        onePeriodScores: [9.0],
        semesterScore: 9.5,
      ),
      SubjectGrade(
        subject: 'Ngữ văn',
        oralScores: [10.0, 10.0, 8.0, 10.0],
        fifteenMinScores: [8.0],
        onePeriodScores: [7.5],
        semesterScore: 8.5,
      ),
      SubjectGrade(
        subject: 'Vật lí',
        oralScores: [9.0, 9.0],
        fifteenMinScores: [8.8],
        onePeriodScores: [9.5],
        semesterScore: 9.2,
      ),
      SubjectGrade(
        subject: 'Sinh học',
        oralScores: [10.0, 9.0, 10.0],
        fifteenMinScores: [9.3],
        onePeriodScores: [9.5],
        semesterScore: 9.5,
      ),
      SubjectGrade(
        subject: 'Tin học',
        oralScores: [10.0, 9.0, 10.0],
        fifteenMinScores: [8.8],
        onePeriodScores: [10.0],
        semesterScore: 9.6,
      ),
      SubjectGrade(
        subject: 'Lịch sử',
        oralScores: [8.0, 8.0],
        fifteenMinScores: [10.0],
        onePeriodScores: [9.0],
        semesterScore: 9.0,
      ),
      SubjectGrade(
        subject: 'Địa lí',
        oralScores: [9.0, 7.0],
        fifteenMinScores: [7.8],
        onePeriodScores: [9.5],
        semesterScore: 8.6,
      ),
      SubjectGrade(
        subject: 'Ngoại ngữ',
        oralScores: [10.0, 10.0, 9.0, 8.0],
        fifteenMinScores: [8.3],
        onePeriodScores: [9.0],
        semesterScore: 9.0,
      ),
      SubjectGrade(
        subject: 'GDCD',
        oralScores: [10.0, 10.0],
        fifteenMinScores: [10.0],
        onePeriodScores: [8.5],
        semesterScore: 9.4,
      ),
      SubjectGrade(
        subject: 'Công nghệ',
        oralScores: [10.0, 9.0, 10.0],
        fifteenMinScores: [9.8],
        onePeriodScores: [10.0],
        semesterScore: 9.8,
      ),
      SubjectGrade(
        subject: 'Thể dục',
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
      SubjectGrade(
        subject: 'Âm nhạc',
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
      SubjectGrade(
        subject: 'Mĩ thuật',
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
    ];
  }

  double get _overallGPA {
    double sum = 0;
    int count = 0;
    for (var g in _grades) {
      var val = g.tbmDouble;
      if (val != null) {
        sum += val;
        count++;
      }
    }
    if (count == 0) return 0.0;
    return double.parse((sum / count).toStringAsFixed(1));
  }

  String get _academicLevel {
    double gpa = _overallGPA;
    bool hasSubjectUnder6_5 = false;
    bool hasSubjectUnder5_0 = false;
    bool hasSubjectUnder3_5 = false;
    bool allCommentsPass = true;

    for (var g in _grades) {
      if (g.isCommentBased) {
        if (g.tbm != 'Đạt') {
          allCommentsPass = false;
        }
      } else {
        double? tbmVal = g.tbmDouble;
        if (tbmVal != null) {
          if (tbmVal < 6.5) hasSubjectUnder6_5 = true;
          if (tbmVal < 5.0) hasSubjectUnder5_0 = true;
          if (tbmVal < 3.5) hasSubjectUnder3_5 = true;
        }
      }
    }

    if (gpa >= 8.0 && !hasSubjectUnder6_5 && allCommentsPass) {
      return 'Tốt';
    } else if (gpa >= 6.5 && !hasSubjectUnder5_0 && allCommentsPass) {
      return 'Khá';
    } else if (gpa >= 5.0 && !hasSubjectUnder3_5 && allCommentsPass) {
      return 'Đạt';
    } else {
      return 'Chưa đạt';
    }
  }

  String get _honorTitle {
    String level = _academicLevel;
    if (level == 'Tốt' && _conduct == 'Tốt') {
      return _overallGPA >= 9.0 ? 'Học sinh Xuất sắc' : 'Học sinh Giỏi';
    } else if ((level == 'Tốt' || level == 'Khá') && (_conduct == 'Tốt' || _conduct == 'Khá')) {
      return 'Học sinh Tiêu biểu';
    } else {
      return 'Không';
    }
  }

  void _showEditGradeSheet(SubjectGrade grade, int index) {
    if (grade.isCommentBased) {
      _showCommentEditSheet(grade, index);
    } else {
      _showNumericalEditSheet(grade, index);
    }
  }

  void _showNumericalEditSheet(SubjectGrade grade, int index) {
    final oralController = TextEditingController(text: grade.oralScores.map((s) => s.toString()).join(', '));
    final fifteenController = TextEditingController(text: grade.fifteenMinScores.map((s) => s.toString()).join(', '));
    final onePeriodController = TextEditingController(text: grade.onePeriodScores.map((s) => s.toString()).join(', '));
    final semesterController = TextEditingController(text: grade.semesterScore.toString());
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
                        'Mô phỏng điểm: ${grade.subject}',
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
                          onPressed: () {
                            setState(() {
                              _grades[index] = _grades[index].copyWith(
                                oralScores: [],
                                fifteenMinScores: [],
                                onePeriodScores: [],
                                semesterScore: 0.0,
                              );
                            });
                            Navigator.pop(context);
                          },
                          style: OutlinedButton.styleFrom(
                            side: const BorderSide(color: AppColors.danger),
                          ),
                          child: const Text('Xóa hết điểm', style: TextStyle(color: AppColors.danger, fontWeight: FontWeight.bold)),
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
                                _grades[index] = _grades[index].copyWith(
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
                          child: const Text('Áp dụng', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
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

  void _showCommentEditSheet(SubjectGrade grade, int index) {
    List<String> tempOral = List.from(grade.commentOralScores);
    List<String> tempFifteen = List.from(grade.commentFifteenMinScores);
    List<String> tempOnePeriod = List.from(grade.commentOnePeriodScores);
    String tempSemester = grade.commentSemesterScore;

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
                        'Mô phỏng điểm nhận xét: ${grade.subject}',
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
                          onPressed: () {
                            setSheetState(() {
                              tempOral = List.filled(tempOral.length, 'KĐ');
                              tempFifteen = List.filled(tempFifteen.length, 'KĐ');
                              tempOnePeriod = List.filled(tempOnePeriod.length, 'KĐ');
                              tempSemester = 'KĐ';
                            });
                          },
                          style: OutlinedButton.styleFrom(
                            side: const BorderSide(color: AppColors.danger),
                          ),
                          child: const Text('Đánh dấu KĐ hết', style: TextStyle(color: AppColors.danger, fontWeight: FontWeight.bold)),
                        ),
                      ),
                      const SizedBox(width: AppSpacing.md),
                      Expanded(
                        child: ElevatedButton(
                          onPressed: () {
                            setState(() {
                              _grades[index] = _grades[index].copyWith(
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
                          child: const Text('Áp dụng', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
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
        if (val == null || val.trim().isEmpty) return null; // Allow empty
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
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text(
          'Bảng kết quả học tập',
          style: TextStyle(fontWeight: FontWeight.w900, fontSize: 16, color: Colors.white),
        ),
        backgroundColor: AppColors.fptOrange,
        iconTheme: const IconThemeData(color: Colors.white),
        elevation: 0,
        actions: [
          Row(
            children: [
              const Text('Mô phỏng', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12)),
              Switch(
                value: _isSimulationMode,
                activeThumbColor: Colors.white,
                activeTrackColor: AppColors.green,
                onChanged: (val) {
                  setState(() {
                    _isSimulationMode = val;
                    if (!val) {
                      _resetGrades(); // Reset to official grades when toggled off
                      _conduct = 'Tốt';
                    }
                  });
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(val
                          ? 'Đã bật Chế độ mô phỏng! Nhấp vào các ô điểm để chỉnh sửa.'
                          : 'Đã tắt Chế độ mô phỏng! Dữ liệu được khôi phục về gốc.'),
                      behavior: SnackBarBehavior.floating,
                      duration: const Duration(seconds: 2),
                    ),
                  );
                },
              ),
            ],
          )
        ],
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            if (_isSimulationMode)
              Container(
                margin: const EdgeInsets.only(bottom: AppSpacing.md),
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.primarySoft,
                  border: Border.all(color: AppColors.fptOrange),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.info, color: AppColors.fptOrange),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Chế độ mô phỏng điểm số',
                            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppColors.fptOrange),
                          ),
                          const SizedBox(height: 2),
                          const Text(
                            'Hãy nhấn vào bất kỳ ô điểm nào để sửa đổi và xem GPA cùng xếp hạng Học lực cập nhật trực tiếp.',
                            style: TextStyle(fontSize: 11.5, color: AppColors.ink),
                          ),
                          const SizedBox(height: 6),
                          Row(
                            children: [
                              const Text('Mô phỏng Hạnh kiểm: ', style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.bold, color: AppColors.ink)),
                              const SizedBox(width: 8),
                              DropdownButton<String>(
                                value: _conduct,
                                items: ['Tốt', 'Khá', 'Trung bình', 'Yếu'].map((String val) {
                                  return DropdownMenuItem<String>(
                                    value: val,
                                    child: Text(val, style: const TextStyle(fontSize: 11.5)),
                                  );
                                }).toList(),
                                onChanged: (newVal) {
                                  if (newVal != null) {
                                    setState(() {
                                      _conduct = newVal;
                                    });
                                  }
                                },
                              )
                            ],
                          )
                        ],
                      ),
                    ),
                  ],
                ),
              ),

            // Semester Selector Card
            AppCard(
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: const [
                  Icon(Icons.arrow_left, size: 28, color: AppColors.fptOrange),
                  Text(
                    'HỌC KỲ II - NĂM HỌC 2026-2027',
                    style: TextStyle(fontSize: 14, fontWeight: FontWeight.w900, color: AppColors.ink),
                  ),
                  Icon(Icons.arrow_right, size: 28, color: AppColors.fptOrange),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.md),

            // Summary Table Card
            Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppColors.line),
              ),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: Column(
                  children: [
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
                      color: AppColors.fptOrange,
                      child: Text(
                        _isSimulationMode ? 'Kết quả học tập (Mô phỏng)' : 'Kết quả học tập trong học kỳ 2',
                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
                      ),
                    ),
                    Table(
                      border: TableBorder.symmetric(inside: const BorderSide(color: AppColors.line)),
                      children: [
                        TableRow(children: [
                          const TableCell(child: Padding(padding: EdgeInsets.all(12), child: Text('Điểm TB', style: TextStyle(fontSize: 12.5, color: AppColors.ink)))),
                          TableCell(
                            child: Padding(
                              padding: const EdgeInsets.all(12),
                              child: Text(
                                _overallGPA.toStringAsFixed(1),
                                style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.bold, color: AppColors.fptOrange),
                              ),
                            ),
                          ),
                        ]),
                        TableRow(children: [
                          const TableCell(child: Padding(padding: EdgeInsets.all(12), child: Text('Xếp hạng lớp', style: TextStyle(fontSize: 12.5, color: AppColors.ink)))),
                          TableCell(
                            child: Padding(
                              padding: const EdgeInsets.all(12),
                              child: Text(
                                '$_rank',
                                style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.bold, color: AppColors.ink),
                              ),
                            ),
                          ),
                        ]),
                        TableRow(children: [
                          const TableCell(child: Padding(padding: EdgeInsets.all(12), child: Text('Danh hiệu thi đua', style: TextStyle(fontSize: 12.5, color: AppColors.ink)))),
                          TableCell(
                            child: Padding(
                              padding: const EdgeInsets.all(12),
                              child: Text(
                                _honorTitle,
                                style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.bold, color: AppColors.green),
                              ),
                            ),
                          ),
                        ]),
                        TableRow(children: [
                          const TableCell(child: Padding(padding: EdgeInsets.all(12), child: Text('Hạnh kiểm', style: TextStyle(fontSize: 12.5, color: AppColors.ink)))),
                          TableCell(
                            child: Padding(
                              padding: const EdgeInsets.all(12),
                              child: Text(
                                _conduct,
                                style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.bold, color: AppColors.ink),
                              ),
                            ),
                          ),
                        ]),
                        TableRow(children: [
                          const TableCell(child: Padding(padding: EdgeInsets.all(12), child: Text('Học lực', style: TextStyle(fontSize: 12.5, color: AppColors.ink)))),
                          TableCell(
                            child: Padding(
                              padding: const EdgeInsets.all(12),
                              child: Text(
                                _academicLevel,
                                style: TextStyle(
                                  fontSize: 12.5,
                                  fontWeight: FontWeight.bold,
                                  color: _academicLevel == 'Tốt' || _academicLevel == 'Khá' ? AppColors.green : AppColors.danger,
                                ),
                              ),
                            ),
                          ),
                        ]),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.md),

            // Detailed Grades Card
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
                      child: const Text(
                        'Bảng điểm các môn học kỳ 2 (Thông tư 22)',
                        style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
                      ),
                    ),
                    SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      child: DataTable(
                        columnSpacing: 18,
                        horizontalMargin: 12,
                        columns: const [
                          DataColumn(label: Text('Môn học', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppColors.ink))),
                          DataColumn(label: Text('Điểm miệng', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppColors.ink))),
                          DataColumn(label: Text('Điểm 15p', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppColors.ink))),
                          DataColumn(label: Text('Điểm 1 tiết', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppColors.ink))),
                          DataColumn(label: Text('Học kỳ', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppColors.ink))),
                          DataColumn(label: Text('TBM', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppColors.ink))),
                        ],
                        rows: List.generate(_grades.length, (index) {
                          final g = _grades[index];
                          final isGraded = !g.isCommentBased;

                          final oralText = isGraded
                              ? (g.oralScores.isEmpty ? '-' : g.oralScores.map((s) => s.toString().replaceAll('.0', '')).join('  '))
                              : (g.commentOralScores.isEmpty ? '-' : g.commentOralScores.join('  '));

                          final fifteenText = isGraded
                              ? (g.fifteenMinScores.isEmpty ? '-' : g.fifteenMinScores.map((s) => s.toString().replaceAll('.0', '')).join('  '))
                              : (g.commentFifteenMinScores.isEmpty ? '-' : g.commentFifteenMinScores.join('  '));

                          final onePeriodText = isGraded
                              ? (g.onePeriodScores.isEmpty ? '-' : g.onePeriodScores.map((s) => s.toString().replaceAll('.0', '')).join('  '))
                              : (g.commentOnePeriodScores.isEmpty ? '-' : g.commentOnePeriodScores.join('  '));

                          final semesterText = isGraded
                              ? (g.semesterScore == 0.0 ? '-' : g.semesterScore.toString().replaceAll('.0', ''))
                              : (g.commentSemesterScore.isEmpty ? '-' : g.commentSemesterScore);

                          final tbmValue = g.tbm;
                          final isPass = tbmValue == 'Đạt' || tbmValue == 'Đ' || (double.tryParse(tbmValue) ?? 0) >= 8.0;

                          return DataRow(
                            cells: [
                              DataCell(
                                Text(g.subject, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12.5, color: AppColors.ink)),
                              ),
                              DataCell(
                                InkWell(
                                  onTap: _isSimulationMode ? () => _showEditGradeSheet(g, index) : null,
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                                    decoration: _isSimulationMode
                                        ? BoxDecoration(
                                            border: Border(bottom: BorderSide(style: BorderStyle.solid, width: 1.0, color: Colors.grey.shade400)),
                                          )
                                        : null,
                                    child: Text(oralText, style: const TextStyle(fontSize: 12, color: AppColors.muted)),
                                  ),
                                ),
                              ),
                              DataCell(
                                InkWell(
                                  onTap: _isSimulationMode ? () => _showEditGradeSheet(g, index) : null,
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                                    decoration: _isSimulationMode
                                        ? BoxDecoration(
                                            border: Border(bottom: BorderSide(style: BorderStyle.solid, width: 1.0, color: Colors.grey.shade400)),
                                          )
                                        : null,
                                    child: Text(fifteenText, style: const TextStyle(fontSize: 12, color: AppColors.muted)),
                                  ),
                                ),
                              ),
                              DataCell(
                                InkWell(
                                  onTap: _isSimulationMode ? () => _showEditGradeSheet(g, index) : null,
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                                    decoration: _isSimulationMode
                                        ? BoxDecoration(
                                            border: Border(bottom: BorderSide(style: BorderStyle.solid, width: 1.0, color: Colors.grey.shade400)),
                                          )
                                        : null,
                                    child: Text(onePeriodText, style: const TextStyle(fontSize: 12, color: AppColors.muted)),
                                  ),
                                ),
                              ),
                              DataCell(
                                InkWell(
                                  onTap: _isSimulationMode ? () => _showEditGradeSheet(g, index) : null,
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                                    decoration: _isSimulationMode
                                        ? BoxDecoration(
                                            border: Border(bottom: BorderSide(style: BorderStyle.solid, width: 1.0, color: Colors.grey.shade400)),
                                          )
                                        : null,
                                    child: Text(semesterText, style: const TextStyle(fontSize: 12, color: AppColors.muted)),
                                  ),
                                ),
                              ),
                              DataCell(
                                Text(
                                  tbmValue,
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 12.5,
                                    color: isPass ? AppColors.green : AppColors.fptOrange,
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
            const SizedBox(height: AppSpacing.md),

            // Homeroom Teacher Comment Card
            const SectionHeader(title: 'Nhận xét của Giáo viên chủ nhiệm'),
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: const [
                  Text(
                    'Cô Nguyễn Thu Hà:',
                    style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.ink),
                  ),
                  SizedBox(height: AppSpacing.xs),
                  Text(
                    'Học sinh có ý thức học tập tốt, đạt kết quả cao ở môn Toán và Lịch sử. Cần rèn luyện thêm kỹ năng giao tiếp môn Ngoại ngữ để hoàn thiện hơn.',
                    style: TextStyle(fontSize: 12.5, color: AppColors.muted, height: 1.35),
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
