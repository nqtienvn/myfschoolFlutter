import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class TeacherTuitionScreen extends StatefulWidget {
  const TeacherTuitionScreen({super.key});

  @override
  State<TeacherTuitionScreen> createState() => _TeacherTuitionScreenState();
}

class _TeacherTuitionScreenState extends State<TeacherTuitionScreen> {
  String _selectedFilter = 'Tất cả';

  // Compute total stats
  int get totalStudents => mockStudents.length;

  int get paidCount {
    return mockStudents.where((student) {
      final unpaid = student.tuitionBills
          .where((bill) => bill.status == 'Chưa đóng')
          .fold(0, (sum, bill) => sum + bill.amount);
      return unpaid == 0;
    }).length;
  }

  int get unpaidCount => totalStudents - paidCount;

  List<StudentSnapshot> get filteredStudents {
    if (_selectedFilter == 'Đã đóng') {
      return mockStudents.where((student) {
        final unpaid = student.tuitionBills
            .where((bill) => bill.status == 'Chưa đóng')
            .fold(0, (sum, bill) => sum + bill.amount);
        return unpaid == 0;
      }).toList();
    } else if (_selectedFilter == 'Chưa đóng') {
      return mockStudents.where((student) {
        final unpaid = student.tuitionBills
            .where((bill) => bill.status == 'Chưa đóng')
            .fold(0, (sum, bill) => sum + bill.amount);
        return unpaid > 0;
      }).toList();
    }
    return mockStudents;
  }

  void _sendReminder(StudentSnapshot student) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Đã gửi nhắc nhở học phí đến phụ huynh học sinh ${student.name}!'),
        behavior: SnackBarBehavior.floating,
        backgroundColor: AppColors.fptOrange,
      ),
    );
  }



  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Học phí lớp chủ nhiệm'),
      body: SafeArea(
        child: Column(
          children: [
            // Mini Header Stats (Gọn gàng, ít chữ)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
              color: Colors.white,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'Trạng thái đóng phí lớp',
                    style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.ink),
                  ),
                  Text(
                    'Đã đóng: $paidCount / $totalStudents',
                    style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: AppColors.muted),
                  ),
                ],
              ),
            ),
            const Divider(height: 1, color: AppColors.line),

            // Filters (Tabs tối giản)
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 12, 24, 8),
              child: Row(
                children: [
                  for (final filter in ['Tất cả', 'Đã đóng', 'Chưa đóng']) ...[
                    GestureDetector(
                      onTap: () => setState(() => _selectedFilter = filter),
                      child: Container(
                        margin: const EdgeInsets.only(right: 8),
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                        decoration: BoxDecoration(
                          color: _selectedFilter == filter ? AppColors.primarySoft : AppColors.surface,
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(
                            color: _selectedFilter == filter
                                ? AppColors.fptOrange
                                : AppColors.line.withValues(alpha: 0.6),
                          ),
                        ),
                        child: Text(
                          filter,
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.bold,
                            color: _selectedFilter == filter ? AppColors.fptOrange : AppColors.ink,
                          ),
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ),

            // Student list dạng Single Line phẳng
            Expanded(
              child: ListView.builder(
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
                itemCount: filteredStudents.length,
                itemBuilder: (context, index) {
                  final student = filteredStudents[index];
                  final unpaid = student.tuitionBills
                      .where((bill) => bill.status == 'Chưa đóng')
                      .fold(0, (sum, bill) => sum + bill.amount);
                  final isPaid = unpaid == 0;

                  return Semantics(
                    label: 'Học sinh: ${student.name}. Trạng thái học phí: ${isPaid ? 'Đã đóng đủ' : 'Chưa đóng'}.',
                    child: Container(
                      margin: const EdgeInsets.only(bottom: 8),
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                      decoration: BoxDecoration(
                        color: AppColors.surface,
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(color: AppColors.line.withValues(alpha: 0.4)),
                      ),
                      child: Row(
                        children: [
                          // Avatar nhỏ gọn
                          CircleAvatar(
                            radius: 14,
                            backgroundColor: student.avatarColor.withValues(alpha: 0.12),
                            child: Text(
                              student.shortName,
                              style: TextStyle(
                                color: student.avatarColor,
                                fontSize: 10,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                          const SizedBox(width: 12),

                          // Tên học sinh
                          Expanded(
                            child: Text(
                              student.name,
                              style: const TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w600,
                                color: AppColors.ink,
                              ),
                            ),
                          ),

                          // Trạng thái (Đã đóng / Chưa đóng)
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: isPaid ? AppColors.successSoft : AppColors.dangerSoft,
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: Text(
                              isPaid ? 'Đã đóng' : 'Chưa đóng',
                              style: TextStyle(
                                fontSize: 10,
                                fontWeight: FontWeight.bold,
                                color: isPaid ? AppColors.success : AppColors.danger,
                              ),
                            ),
                          ),

                          // Nút nhắc nhở dạng Icon quả chuông tối giản nhưng dễ chạm bấm (A11y friendly)
                          if (!isPaid) ...[
                            const SizedBox(width: 12),
                            Semantics(
                              label: 'Nhấn để gửi nhắc nhở nộp tiền học phí cho phụ huynh học sinh ${student.name}',
                              button: true,
                              child: IconButton(
                                onPressed: () => _sendReminder(student),
                                icon: const Icon(Icons.notifications_active),
                                color: AppColors.fptOrange,
                                iconSize: 20,
                                padding: const EdgeInsets.all(10),
                                constraints: const BoxConstraints(minWidth: 44, minHeight: 44),
                                tooltip: 'Nhắc đóng học phí',
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class CardTile extends StatelessWidget {
  const CardTile({
    super.key,
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
    required this.bgColor,
  });

  final String label;
  final String value;
  final IconData icon;
  final Color color;
  final Color bgColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.line.withValues(alpha: 0.5)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.02),
            blurRadius: 6,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                label,
                style: const TextStyle(
                  fontSize: 11,
                  color: AppColors.muted,
                  fontWeight: FontWeight.bold,
                ),
              ),
              Container(
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                  color: bgColor,
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: color, size: 16),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            value,
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w900,
              color: color,
            ),
          ),
        ],
      ),
    );
  }
}
