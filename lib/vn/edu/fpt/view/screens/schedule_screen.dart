import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class ScheduleScreen extends StatefulWidget {
  const ScheduleScreen({super.key});

  @override
  State<ScheduleScreen> createState() => _ScheduleScreenState();
}

class _ScheduleScreenState extends State<ScheduleScreen> {
  String _selectedDay = 'T4';

  @override
  Widget build(BuildContext context) {
    const morningLessons = [
      _Lesson(time: '07:30', period: 'Tiết 1', subject: 'Toán học', teacher: 'Cô Nguyễn Thu Hà', room: 'Phòng A204', color: AppColors.blue),
      _Lesson(time: '08:25', period: 'Tiết 2', subject: 'Ngữ văn', teacher: 'Thầy Lê Minh Đức', room: 'Phòng A204', color: AppColors.green),
      _Lesson(time: '09:25', period: 'Tiết 3', subject: 'Tin học', teacher: 'Thầy Trần Quốc Huy', room: 'Phòng Lab 3', color: AppColors.fptOrange),
    ];

    const afternoonLessons = [
      _Lesson(time: '13:30', period: 'Tiết 4', subject: 'Vật lý', teacher: 'Thầy Vũ Văn Bính', room: 'Phòng lý', color: Colors.purple),
      _Lesson(time: '14:25', period: 'Tiết 5', subject: 'Tiếng Anh', teacher: 'Cô Lê Thị Nga', room: 'Phòng A204', color: AppColors.blue),
    ];

    final weekDays = [
      ('T2', '15'),
      ('T3', '16'),
      ('T4', '17'),
      ('T5', '18'),
      ('T6', '19'),
      ('T7', '20'),
    ];

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Thời khóa biểu lớp'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          children: [
            // Month Selector Card (Tối giản hơn)
            AppCard(
              padding: 12,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: const [
                  Icon(Icons.chevron_left, size: 24, color: AppColors.fptOrange),
                  Text(
                    'Tháng 06 / 2026',
                    style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.ink),
                  ),
                  Icon(Icons.chevron_right, size: 24, color: AppColors.fptOrange),
                ],
              ),
            ),
            const SizedBox(height: 12),

            // Week Day Selector
            AppCard(
              padding: 12,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: weekDays.map((day) {
                  final isSelected = _selectedDay == day.$1;
                  final weekdayFullName = day.$1 == 'T2' ? 'Thứ hai' : day.$1 == 'T3' ? 'Thứ ba' : day.$1 == 'T4' ? 'Thứ tư' : day.$1 == 'T5' ? 'Thứ năm' : day.$1 == 'T6' ? 'Thứ sáu' : 'Thứ bảy';
                  return Semantics(
                    button: true,
                    selected: isSelected,
                    label: '$weekdayFullName, ngày ${day.$2} tháng 6',
                    child: InkWell(
                      onTap: () => setState(() => _selectedDay = day.$1),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                        decoration: BoxDecoration(
                          color: isSelected ? AppColors.fptOrange : Colors.transparent,
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Column(
                          children: [
                            Text(
                              day.$1,
                              style: TextStyle(
                                fontSize: 10,
                                color: isSelected ? Colors.white : AppColors.muted,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              day.$2,
                              style: TextStyle(
                                fontSize: 14,
                                color: isSelected ? Colors.white : AppColors.ink,
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  );
                }).toList(),
              ),
            ),
            const SizedBox(height: 20),

            // Timeline Buổi sáng
            _buildTimelineSection('Buổi Sáng', morningLessons),
            const SizedBox(height: 24),

            // Timeline Buổi chiều
            _buildTimelineSection('Buổi Chiều', afternoonLessons),
          ],
        ),
      ),
    );
  }

  Widget _buildTimelineSection(String title, List<_Lesson> lessons) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 12),
          child: Row(
            children: [
              Container(
                width: 4,
                height: 16,
                decoration: BoxDecoration(
                  color: AppColors.fptOrange,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(width: 8),
              Text(
                title,
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w900,
                  color: AppColors.ink,
                  letterSpacing: 0.5,
                ),
              ),
            ],
          ),
        ),
        AppCard(
          padding: 12,
          child: Column(
            children: List.generate(lessons.length, (index) {
              final lesson = lessons[index];
              final isLast = index == lessons.length - 1;

              return Semantics(
                label: '${lesson.period} bắt đầu lúc ${lesson.time}, môn ${lesson.subject}, tại ${lesson.room}, giảng dạy bởi ${lesson.teacher}.',
                child: IntrinsicHeight(
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // Cột 1: Thời gian & Tiết học
                      SizedBox(
                        width: 64,
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(
                              lesson.time,
                              style: const TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w900,
                                color: AppColors.ink,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              lesson.period,
                              style: const TextStyle(
                                fontSize: 10,
                                fontWeight: FontWeight.bold,
                                color: AppColors.muted,
                              ),
                            ),
                          ],
                        ),
                      ),

                      // Cột 2: Spine Line & Dot (Đường thẳng dọc và chấm tròn)
                      SizedBox(
                        width: 24,
                        child: Stack(
                          alignment: Alignment.topCenter,
                          children: [
                            // Dòng kẻ nối dọc
                            if (!isLast)
                              const VerticalDivider(
                                width: 1.5,
                                thickness: 1.5,
                                color: AppColors.line,
                              ),
                            // Chấm tròn mốc thời gian
                            Positioned(
                              top: 14,
                              child: Container(
                                width: 10,
                                height: 10,
                                decoration: BoxDecoration(
                                  color: lesson.color.withValues(alpha: 0.2),
                                  shape: BoxShape.circle,
                                  border: Border.all(color: lesson.color, width: 2),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),

                      // Cột 3: Nội dung chi tiết môn học
                      Expanded(
                        child: Container(
                          margin: const EdgeInsets.symmetric(vertical: 6),
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: lesson.color.withValues(alpha: 0.05),
                            borderRadius: BorderRadius.circular(8),
                            border: Border(
                              left: BorderSide(color: lesson.color, width: 3),
                            ),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Text(
                                    lesson.subject,
                                    style: const TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.bold,
                                      color: AppColors.ink,
                                    ),
                                  ),
                                  Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                    decoration: BoxDecoration(
                                      color: Colors.white,
                                      borderRadius: BorderRadius.circular(4),
                                      border: Border.all(color: AppColors.line.withValues(alpha: 0.6)),
                                    ),
                                    child: Text(
                                      lesson.room,
                                      style: TextStyle(
                                        fontSize: 9,
                                        fontWeight: FontWeight.w700,
                                        color: lesson.color,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 4),
                              Text(
                                lesson.teacher,
                                style: const TextStyle(
                                  fontSize: 11,
                                  color: AppColors.muted,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }),
          ),
        ),
      ],
    );
  }
}

class _Lesson {
  const _Lesson({
    required this.time,
    required this.period,
    required this.subject,
    required this.teacher,
    required this.room,
    required this.color,
  });

  final String time;
  final String period;
  final String subject;
  final String teacher;
  final String room;
  final Color color;
}
