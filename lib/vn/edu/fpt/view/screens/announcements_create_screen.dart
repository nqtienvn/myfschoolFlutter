import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_radius.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/primary_button.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class AnnouncementsCreateScreen extends StatefulWidget {
  const AnnouncementsCreateScreen({super.key});

  @override
  State<AnnouncementsCreateScreen> createState() => _AnnouncementsCreateScreenState();
}

class _AnnouncementsCreateScreenState extends State<AnnouncementsCreateScreen> {
  final _titleController = TextEditingController();
  final _contentController = TextEditingController();
  String _selectedClass = '12A';

  @override
  void dispose() {
    _titleController.dispose();
    _contentController.dispose();
    super.dispose();
  }

  void _sendAnnouncement() {
    if (_titleController.text.trim().isEmpty || _contentController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng điền đầy đủ tiêu đề và nội dung thông báo.'),
          behavior: SnackBarBehavior.floating,
        ),
      );
      return;
    }

    Navigator.of(context).pop();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Đã gửi thông báo đến phụ huynh lớp $_selectedClass!'),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Tạo thông báo mới'),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Chọn đối tượng nhận',
                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.muted),
                ),
                const SizedBox(height: AppSpacing.sm),
                Row(
                  children: ['12A', 'SE1913', '11B'].map((classOption) {
                    final isSelected = _selectedClass == classOption;
                    return Padding(
                      padding: const EdgeInsets.only(right: AppSpacing.sm),
                      child: ChoiceChip(
                        label: Text(classOption),
                        selected: isSelected,
                        onSelected: (_) {
                          setState(() => _selectedClass = classOption);
                        },
                        selectedColor: AppColors.primarySoft,
                        labelStyle: TextStyle(
                          color: isSelected ? AppColors.fptOrange : AppColors.ink,
                          fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                        ),
                      ),
                    );
                  }).toList(),
                ),
                const SizedBox(height: AppSpacing.lg),
                const Text(
                  'Tiêu đề thông báo',
                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.muted),
                ),
                const SizedBox(height: AppSpacing.sm),
                TextField(
                  controller: _titleController,
                  decoration: InputDecoration(
                    hintText: 'Nhập tiêu đề...',
                    prefixIcon: const Icon(Icons.title_outlined),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(AppRadius.md),
                    ),
                  ),
                ),
                const SizedBox(height: AppSpacing.lg),
                const Text(
                  'Nội dung chi tiết',
                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.muted),
                ),
                const SizedBox(height: AppSpacing.sm),
                TextField(
                  controller: _contentController,
                  minLines: 5,
                  maxLines: 8,
                  decoration: InputDecoration(
                    hintText: 'Nhập nội dung thông báo gửi phụ huynh...',
                    prefixIcon: const Icon(Icons.edit_note_outlined),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(AppRadius.md),
                    ),
                  ),
                ),
                const SizedBox(height: AppSpacing.xl),
                PrimaryButton(
                  label: 'Gửi thông báo',
                  icon: Icons.send_outlined,
                  onPressed: _sendAnnouncement,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
