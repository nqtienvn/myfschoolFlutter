import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
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



  void _createTuitionRequest(StudentSnapshot student) {
    final formKey = GlobalKey<FormState>();
    String requestType = 'Đơn xin gia hạn đóng học phí';
    String reason = '';
    String additionalInfo = '';

    showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setStateDialog) {
            return AlertDialog(
              title: Text(
                'Lập đơn học phí hộ HS',
                style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16),
              ),
              content: Form(
                key: formKey,
                child: SingleChildScrollView(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Học sinh: ${student.name}',
                        style: const TextStyle(fontSize: 12, color: AppColors.muted, fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 12),

                      // Request Type
                      const Text(
                        'Loại đơn từ học phí',
                        style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.ink),
                      ),
                      const SizedBox(height: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                        decoration: BoxDecoration(
                          color: AppColors.background,
                          border: Border.all(color: AppColors.line),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: DropdownButton<String>(
                          value: requestType,
                          isExpanded: true,
                          underline: const SizedBox(),
                          items: const [
                            DropdownMenuItem(
                              value: 'Đơn xin gia hạn đóng học phí',
                              child: Text('Xin gia hạn đóng học phí', style: TextStyle(fontSize: 13)),
                            ),
                            DropdownMenuItem(
                              value: 'Đơn xin miễn giảm học phí',
                              child: Text('Xin miễn giảm học phí', style: TextStyle(fontSize: 13)),
                            ),
                            DropdownMenuItem(
                              value: 'Đơn báo cáo lỗi thanh toán',
                              child: Text('Báo cáo lỗi thanh toán', style: TextStyle(fontSize: 13)),
                            ),
                          ],
                          onChanged: (val) {
                            if (val != null) {
                              setStateDialog(() {
                                requestType = val;
                              });
                            }
                          },
                        ),
                      ),
                      const SizedBox(height: 12),

                      // Dynamic Fields based on Request Type
                      if (requestType == 'Đơn xin gia hạn đóng học phí') ...[
                        const Text(
                          'Thời gian xin gia hạn',
                          style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.ink),
                        ),
                        const SizedBox(height: 6),
                        TextFormField(
                          initialValue: '15 ngày',
                          decoration: const InputDecoration(
                            hintText: 'Nhập số ngày hoặc ngày cụ thể...',
                            contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                          ),
                          validator: (value) => value == null || value.isEmpty ? 'Vui lòng điền thông tin' : null,
                          onSaved: (value) => additionalInfo = value ?? '',
                        ),
                      ] else if (requestType == 'Đơn xin miễn giảm học phí') ...[
                        const Text(
                          'Tỷ lệ miễn giảm đề xuất (%)',
                          style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.ink),
                        ),
                        const SizedBox(height: 6),
                        TextFormField(
                          initialValue: '30%',
                          decoration: const InputDecoration(
                            hintText: 'Nhập tỷ lệ phần trăm (ví dụ: 50%)...',
                            contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                          ),
                          validator: (value) => value == null || value.isEmpty ? 'Vui lòng điền thông tin' : null,
                          onSaved: (value) => additionalInfo = value ?? '',
                        ),
                      ] else ...[
                        const Text(
                          'Mã giao dịch / Ngày chuyển',
                          style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.ink),
                        ),
                        const SizedBox(height: 6),
                        TextFormField(
                          decoration: const InputDecoration(
                            hintText: 'Ví dụ: FT260624001, Techcombank...',
                            contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                          ),
                          validator: (value) => value == null || value.isEmpty ? 'Vui lòng điền thông tin' : null,
                          onSaved: (value) => additionalInfo = value ?? '',
                        ),
                      ],
                      const SizedBox(height: 12),

                      // Reason Field
                      const Text(
                        'Lý do lập đơn',
                        style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.ink),
                      ),
                      const SizedBox(height: 6),
                      TextFormField(
                        maxLines: 3,
                        decoration: const InputDecoration(
                          hintText: 'Nhập lý do chi tiết trình Ban giám hiệu...',
                          contentPadding: EdgeInsets.all(12),
                        ),
                        validator: (value) => value == null || value.isEmpty ? 'Vui lòng nhập lý do' : null,
                        onSaved: (value) => reason = value ?? '',
                      ),
                    ],
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('Hủy'),
                ),
                ElevatedButton(
                  onPressed: () {
                    if (formKey.currentState?.validate() ?? false) {
                      formKey.currentState?.save();
                      Navigator.pop(context);

                      // Simulate adding the request to the student's leaveRequests list
                      setState(() {
                        student.leaveRequests.insert(
                          0,
                          LeaveRequest(
                            title: requestType,
                            date: 'Gửi hôm nay (Hộ)',
                            reason: '$reason ($additionalInfo)',
                            status: 'Pending',
                            statusColor: AppColors.warning,
                            statusBackground: AppColors.warningSoft,
                            note: 'Giáo viên lập hộ, đang trình duyệt.',
                          ),
                        );
                      });

                      showDialog<void>(
                        context: context,
                        builder: (context) => AlertDialog(
                          title: const Text('Đã gửi đơn', style: TextStyle(fontWeight: FontWeight.bold)),
                          content: Text('Đơn từ liên quan đến học phí của học sinh ${student.name} đã được tạo và gửi trình duyệt thành công!'),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.pop(context),
                              child: const Text('Đóng'),
                            ),
                          ],
                        ),
                      );
                    }
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.fptOrange,
                    foregroundColor: Colors.white,
                  ),
                  child: const Text('Gửi duyệt'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Quản lý Học phí lớp'),
      body: SafeArea(
        child: Column(
          children: [
            // Stats Row
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 20, 24, 0),
              child: Row(
                children: [
                  Expanded(
                    child: CardTile(
                      label: 'Đã hoàn thành',
                      value: '$paidCount / $totalStudents',
                      icon: Icons.check_circle_outline,
                      color: AppColors.success,
                      bgColor: AppColors.successSoft,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: CardTile(
                      label: 'Chưa hoàn thành',
                      value: '$unpaidCount / $totalStudents',
                      icon: Icons.error_outline,
                      color: AppColors.danger,
                      bgColor: AppColors.dangerSoft,
                    ),
                  ),
                ],
              ),
            ),

            // Filters
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
              child: Row(
                children: [
                  for (final filter in ['Tất cả', 'Đã đóng', 'Chưa đóng']) ...[
                    GestureDetector(
                      onTap: () => setState(() => _selectedFilter = filter),
                      child: Container(
                        margin: const EdgeInsets.only(right: 8),
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                        decoration: BoxDecoration(
                          color: _selectedFilter == filter ? AppColors.primarySoft : AppColors.surface,
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(
                            color: _selectedFilter == filter
                                ? AppColors.fptOrange
                                : AppColors.line.withValues(alpha: 0.6),
                          ),
                        ),
                        child: Text(
                          filter,
                          style: TextStyle(
                            fontSize: 12,
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

            // Student list
            Expanded(
              child: ListView.builder(
                padding: const EdgeInsets.symmetric(horizontal: 24),
                itemCount: filteredStudents.length,
                itemBuilder: (context, index) {
                  final student = filteredStudents[index];
                  final unpaid = student.tuitionBills
                      .where((bill) => bill.status == 'Chưa đóng')
                      .fold(0, (sum, bill) => sum + bill.amount);
                  final isPaid = unpaid == 0;

                  return Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: AppCard(
                      child: Column(
                        children: [
                          Row(
                            children: [
                              CircleAvatar(
                                radius: 18,
                                backgroundColor: student.avatarColor.withValues(alpha: 0.12),
                                child: Text(
                                  student.shortName,
                                  style: TextStyle(
                                    color: student.avatarColor,
                                    fontSize: 11,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      student.name,
                                      style: const TextStyle(
                                        fontSize: 14,
                                        fontWeight: FontWeight.bold,
                                        color: AppColors.ink,
                                      ),
                                    ),
                                    const SizedBox(height: 2),
                                    Text(
                                      'Mã HS: ${student.studentCode}',
                                      style: const TextStyle(
                                        fontSize: 11,
                                        color: AppColors.muted,
                                        fontWeight: FontWeight.w600,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              Column(
                                crossAxisAlignment: CrossAxisAlignment.end,
                                children: [
                                  Text(
                                    isPaid
                                        ? 'Đã đóng đủ'
                                        : '${unpaid.toString().replaceAllMapped(RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'), (Match m) => '${m[1]}.')} đ',
                                    style: TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.w900,
                                      color: isPaid ? AppColors.success : AppColors.danger,
                                    ),
                                  ),
                                  const SizedBox(height: 2),
                                  StatusPill(
                                    label: isPaid ? 'Đã đóng' : 'Chưa đóng',
                                    foreground: isPaid ? AppColors.success : AppColors.danger,
                                    background: isPaid ? AppColors.successSoft : AppColors.dangerSoft,
                                    compact: true,
                                  ),
                                ],
                              ),
                            ],
                          ),
                          if (!isPaid) ...[
                            const Divider(height: 20),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.end,
                              children: [
                                OutlinedButton.icon(
                                  onPressed: () => _createTuitionRequest(student),
                                  icon: const Icon(Icons.note_add_outlined, size: 16),
                                  label: const Text('Tạo đơn', style: TextStyle(fontSize: 11)),
                                  style: OutlinedButton.styleFrom(
                                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                                    shape: RoundedRectangleBorder(
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                  ),
                                ),
                                const SizedBox(width: 8),
                                ElevatedButton.icon(
                                  onPressed: () => _sendReminder(student),
                                  icon: const Icon(Icons.notifications_active_outlined, size: 16),
                                  label: const Text('Nhắc nhở', style: TextStyle(fontSize: 11)),
                                  style: ElevatedButton.styleFrom(
                                    backgroundColor: AppColors.fptOrange,
                                    foregroundColor: Colors.white,
                                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                                    shape: RoundedRectangleBorder(
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                  ),
                                ),
                              ],
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
