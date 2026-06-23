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
      _Lesson(period: 'Tiết 1 (07:30 - 08:15)', subject: 'Toán học', teacher: 'Cô Nguyễn Thu Hà', room: 'Phòng A204'),
      _Lesson(period: 'Tiết 2 (08:25 - 09:10)', subject: 'Ngữ văn', teacher: 'Thầy Lê Minh Đức', room: 'Phòng A204'),
      _Lesson(period: 'Tiết 3 (09:25 - 10:10)', subject: 'Tin học', teacher: 'Thầy Trần Quốc Huy', room: 'Phòng Lab 3'),
    ];

    const afternoonLessons = [
      _Lesson(period: 'Tiết 4 (13:30 - 14:15)', subject: 'Vật lý', teacher: 'Thầy Vũ Văn Bính', room: 'Phòng lý'),
      _Lesson(period: 'Tiết 5 (14:25 - 15:10)', subject: 'Tiếng Anh', teacher: 'Cô Lê Thị Nga', room: 'Phòng A204'),
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
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            // Month Selector Card
            AppCard(
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: const [
                  Icon(Icons.arrow_left, size: 28, color: AppColors.fptOrange),
                  Text(
                    'Tháng 06 / 2026',
                    style: TextStyle(fontSize: 14, fontWeight: FontWeight.w900, color: AppColors.ink),
                  ),
                  Icon(Icons.arrow_right, size: 28, color: AppColors.fptOrange),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.md),

            // Week Day Selector
            AppCard(
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: weekDays.map((day) {
                  final isSelected = _selectedDay == day.$1;
                  return InkWell(
                    onTap: () => setState(() => _selectedDay = day.$1),
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm, vertical: AppSpacing.xs),
                      decoration: BoxDecoration(
                        color: isSelected ? AppColors.fptOrange : Colors.transparent,
                        borderRadius: BorderRadius.circular(18),
                      ),
                      child: Column(
                        children: [
                          Text(
                            day.$1,
                            style: TextStyle(
                              fontSize: 11,
                              color: isSelected ? Colors.white : AppColors.muted,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            day.$2,
                            style: TextStyle(
                              fontSize: 13,
                              color: isSelected ? Colors.white : AppColors.ink,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }).toList(),
              ),
            ),
            const SizedBox(height: AppSpacing.md),

            // Lessons List
            const SectionHeader(title: 'Lịch học buổi sáng'),
            for (final lesson in morningLessons) ...[
              _LessonCard(lesson: lesson),
              const SizedBox(height: AppSpacing.sm),
            ],
            const SizedBox(height: AppSpacing.md),
            const SectionHeader(title: 'Lịch học buổi chiều'),
            for (final lesson in afternoonLessons) ...[
              _LessonCard(lesson: lesson),
              const SizedBox(height: AppSpacing.sm),
            ],
          ],
        ),
      ),
    );
  }
}

class _LessonCard extends StatelessWidget {
  const _LessonCard({required this.lesson});

  final _Lesson lesson;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(AppSpacing.sm),
            decoration: BoxDecoration(
              color: AppColors.primarySoft,
              borderRadius: BorderRadius.circular(8),
            ),
            child: const Icon(Icons.menu_book_outlined, color: AppColors.fptOrange, size: 20),
          ),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      lesson.subject,
                      style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w900, color: AppColors.ink),
                    ),
                    Text(
                      lesson.room,
                      style: const TextStyle(fontSize: 11.5, color: AppColors.fptOrange, fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.xs),
                Text(
                  lesson.period,
                  style: const TextStyle(fontSize: 11, color: AppColors.muted, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: AppSpacing.xs),
                Text(
                  lesson.teacher,
                  style: const TextStyle(fontSize: 12, color: AppColors.ink),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Lesson {
  const _Lesson({
    required this.period,
    required this.subject,
    required this.teacher,
    required this.room,
  });

  final String period;
  final String subject;
  final String teacher;
  final String room;
}
