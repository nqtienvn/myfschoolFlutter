import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/actor_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/activity_screen_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/activity_screen_spec_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/activity_screen_previews.dart';

// ponytail: split 1317 lines → 4 files (278 + 175 + 416 + 500 lines)

class ActivityScreen extends StatefulWidget {
  const ActivityScreen({super.key, this.activeActor = AppActor.parent});

  final AppActor activeActor;

  @override
  State<ActivityScreen> createState() => _ActivityScreenState();
}

class _ActivityScreenState extends State<ActivityScreen> {
  late int _selectedGroup = groupIndexFor(widget.activeActor);

  @override
  Widget build(BuildContext context) {
    final group = catalogGroups[_selectedGroup];

    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(18, 18, 18, 0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          color: AppColors.primarySoft,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: const Icon(
                          Icons.account_tree_outlined,
                          color: AppColors.fptOrange,
                        ),
                      ),
                      const SizedBox(width: 12),
                      const Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Catalog màn hình',
                              style: TextStyle(
                                fontSize: 24,
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                            SizedBox(height: 4),
                            Text(
                              'Screen ID, UC trace, flow, AC và UI preview từ SRS v3.',
                              style: TextStyle(
                                color: AppColors.muted,
                                fontSize: 12,
                                height: 1.3,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        for (var i = 0; i < catalogGroups.length; i++)
                          Padding(
                            padding: const EdgeInsets.only(right: 8),
                            child: ChoiceChip(
                              selected: _selectedGroup == i,
                              label: Text(catalogGroups[i].label),
                              avatar: Icon(catalogGroups[i].icon, size: 18),
                              onSelected: (_) => setState(() => _selectedGroup = i),
                              selectedColor: AppColors.primarySoft,
                              labelStyle: TextStyle(
                                color: _selectedGroup == i
                                    ? AppColors.fptOrange
                                    : AppColors.ink,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                ],
              ),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 6),
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: StatTile(
                          label: 'Màn hình',
                          value: group.screens.length.toString(),
                          icon: Icons.dashboard_customize_outlined,
                          color: group.color,
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: StatTile(
                          label: 'UC liên quan',
                          value: group.ucCountLabel,
                          icon: Icons.account_tree_outlined,
                          color: AppColors.fptOrange,
                        ),
                      ),
                    ],
                  ),
                  const SectionHeader(title: 'Danh sách màn hình'),
                  for (final spec in group.screens)
                    ScreenSpecCard(
                      spec: spec,
                      onTap: () {
                        Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (_) => SrsScreenDetailScreen(spec: spec),
                          ),
                        );
                      },
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

class SrsScreenDetailScreen extends StatelessWidget {
  const SrsScreenDetailScreen({super.key, required this.spec});

  final SrsScreenSpec spec;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: OrangeTopBar(title: spec.id),
      body: ListView(
        padding: const EdgeInsets.all(18),
        children: [
          SurfacePanel(
            color: spec.color.withValues(alpha: 0.08),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: spec.color.withValues(alpha: 0.16),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Icon(spec.icon, color: spec.color),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        spec.title,
                        style: const TextStyle(
                          color: AppColors.ink,
                          fontSize: 20,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        '${spec.actorLabel} • ${spec.channel}',
                        style: const TextStyle(
                          color: AppColors.muted,
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 8),
                      StatusPill(
                        label: spec.ucs,
                        foreground: spec.color,
                        background: Colors.white,
                        compact: true,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SectionHeader(title: 'Thành phần chính'),
          SurfacePanel(
            child: Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final component in spec.components)
                  StatusPill(
                    label: component,
                    foreground: AppColors.ink,
                    background: AppColors.background,
                    compact: true,
                  ),
              ],
            ),
          ),
          const SectionHeader(title: 'Flow / activity'),
          SurfacePanel(
            child: Column(
              children: [
                for (var i = 0; i < spec.flow.length; i++)
                  FlowStep(
                    index: i + 1,
                    text: spec.flow[i],
                    isLast: i == spec.flow.length - 1,
                    color: spec.color,
                  ),
              ],
            ),
          ),
          const SectionHeader(title: 'Acceptance detail'),
          const SurfacePanel(
            child: Column(
              children: [
                AcceptanceLine(
                  text:
                      'Kiểm tra quyền RBAC và phạm vi dữ liệu trước khi hiển thị.',
                ),
                AcceptanceLine(
                  text:
                      'Validate trường bắt buộc, hiển thị lỗi inline và giữ draft khi phù hợp.',
                ),
                AcceptanceLine(
                  text:
                      'Pending/Approved/Rejected hoặc job/export phải có trạng thái rõ ràng.',
                ),
                AcceptanceLine(
                  text:
                      'Tác vụ nhạy cảm phải có dấu hiệu audit, watermark hoặc nguồn dữ liệu.',
                ),
              ],
            ),
          ),
          const SectionHeader(title: 'UI preview'),
          ScreenMockPreview(spec: spec),
        ],
      ),
    );
  }
}
