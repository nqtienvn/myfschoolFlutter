import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/apps/mobile/theme/fpt_mobile_theme.dart';

enum HomeRole { parent, teacher, schoolStaff, departmentStaff }

class RoleHomeScreen extends StatelessWidget {
  const RoleHomeScreen({super.key, required this.role});

  static const routeName = '/home';

  final HomeRole role;

  @override
  Widget build(BuildContext context) {
    final config = _HomeRoleConfig.forRole(role);

    return Scaffold(
      backgroundColor: FptMobileColors.background,
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(14, 10, 14, 0),
                child: _HomeHeader(config: config),
              ),
            ),
            if (config.filters.isNotEmpty)
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.only(top: 20),
                  child: _FilterPills(filters: config.filters),
                ),
              ),
            SliverToBoxAdapter(
              child: Padding(
                padding: EdgeInsets.fromLTRB(
                  14,
                  config.filters.isEmpty ? 18 : 20,
                  14,
                  0,
                ),
                child: Text(
                  'Các chức năng',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
              sliver: SliverGrid(
                delegate: SliverChildBuilderDelegate((context, index) {
                  return _FeatureTile(feature: config.features[index]);
                }, childCount: config.features.length),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 3,
                  mainAxisSpacing: 18,
                  crossAxisSpacing: 14,
                  childAspectRatio: 0.78,
                ),
              ),
            ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.only(top: 8),
                child: _PageDots(activeIndex: 0),
              ),
            ),
            if (config.notice != null)
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(14, 24, 14, 0),
                  child: _NoticeCard(notice: config.notice!),
                ),
              ),
            if (config.news.isNotEmpty)
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(14, 24, 0, 0),
                  child: _NewsSection(news: config.news),
                ),
              ),
            const SliverToBoxAdapter(child: SizedBox(height: 118)),
          ],
        ),
      ),
      bottomNavigationBar: const _HomeBottomNavigationBar(),
    );
  }
}

class _HomeRoleConfig {
  const _HomeRoleConfig({
    required this.roleLabel,
    required this.title,
    required this.subtitle,
    required this.avatarInitials,
    required this.avatarIcon,
    required this.filters,
    required this.features,
    this.notice,
    this.news = const [],
    this.logoOnlyTitle = false,
    this.showHeaderBadge = false,
  });

  final String roleLabel;
  final String title;
  final String subtitle;
  final String avatarInitials;
  final IconData avatarIcon;
  final List<String> filters;
  final List<_FeatureItem> features;
  final _NoticeItem? notice;
  final List<_NewsItem> news;
  final bool logoOnlyTitle;
  final bool showHeaderBadge;

  static _HomeRoleConfig forRole(HomeRole role) {
    switch (role) {
      case HomeRole.parent:
        return _HomeRoleConfig(
          roleLabel: 'Phụ huynh',
          title: 'FPT Schools',
          subtitle: 'Phụ huynh',
          avatarInitials: 'PH',
          avatarIcon: Icons.person_outline,
          logoOnlyTitle: true,
          filters: const ['Lớp 1A - FPT Schools', 'Lớp 6A - FPT Schools'],
          features: const [
            _FeatureItem(
              label: 'Gửi thông báo học sinh',
              icon: Icons.mail_outline,
            ),
            _FeatureItem(
              label: 'Điểm danh học sinh',
              icon: Icons.check_box_outlined,
              badge: 2,
            ),
            _FeatureItem(
              label: 'Nhập ĐGĐK môn học',
              icon: Icons.list_alt_outlined,
            ),
            _FeatureItem(label: 'Nhập ĐGĐK năng lực', icon: Icons.help_outline),
            _FeatureItem(
              label: 'Hộp thư thông báo',
              icon: Icons.inbox_outlined,
            ),
            _FeatureItem(
              label: 'Lịch sử gửi thông báo',
              icon: Icons.history_outlined,
            ),
          ],
        );
      case HomeRole.teacher:
        return _HomeRoleConfig(
          roleLabel: 'Giáo viên',
          title: 'Giáo viên chủ nhiệm',
          subtitle: 'Lớp 1A - FPT Schools',
          avatarInitials: 'GV',
          avatarIcon: Icons.person_pin_outlined,
          filters: const ['Lớp 1A - FPT Schools', 'Lớp 6A - FPT Schools'],
          features: const [
            _FeatureItem(
              label: 'Gửi thông báo phụ huynh',
              icon: Icons.campaign_outlined,
            ),
            _FeatureItem(
              label: 'Điểm danh học sinh',
              icon: Icons.check_box_outlined,
              badge: 2,
            ),
            _FeatureItem(
              label: 'Nhập ĐGĐK môn học',
              icon: Icons.list_alt_outlined,
            ),
            _FeatureItem(label: 'Nhập ĐGĐK năng lực', icon: Icons.help_outline),
            _FeatureItem(
              label: 'Hộp thư thông báo',
              icon: Icons.inbox_outlined,
              badge: 1,
            ),
            _FeatureItem(
              label: 'Lịch sử gửi thông báo',
              icon: Icons.history_outlined,
            ),
          ],
          notice: _NoticeItem(
            title: 'Lịch sinh hoạt lớp',
            description: 'Đã cập nhật kế hoạch tuần cho lớp 1A.',
            icon: Icons.event_note_outlined,
          ),
        );
      case HomeRole.schoolStaff:
        return _HomeRoleConfig(
          roleLabel: 'Cán bộ nhà trường',
          title: 'Lãnh đạo Trường',
          subtitle: 'Cấp Tiểu học',
          avatarInitials: 'CB',
          avatarIcon: Icons.school_outlined,
          filters: const [
            'FPT Schools - Cấp Tiểu học',
            'FPT Schools - Cấp THCS',
          ],
          features: const [
            _FeatureItem(
              label: 'Gửi thông báo học sinh',
              icon: Icons.mail_outline,
            ),
            _FeatureItem(
              label: 'Gửi thông báo Giáo viên',
              icon: Icons.send_outlined,
            ),
            _FeatureItem(
              label: 'Lịch sử gửi thông báo',
              icon: Icons.history_outlined,
            ),
            _FeatureItem(
              label: 'Điểm danh toàn trường',
              icon: Icons.fact_check_outlined,
              badge: 3,
            ),
            _FeatureItem(
              label: 'Hộp thư thông báo',
              icon: Icons.inbox_outlined,
              badge: 12,
            ),
            _FeatureItem(
              label: 'Thống kê giáo dục',
              icon: Icons.analytics_outlined,
            ),
          ],
          notice: _NoticeItem(
            title: 'Kế hoạch thi học kỳ 1',
            description: 'Đã cập nhật lịch thi chính thức cho toàn trường.',
            icon: Icons.campaign_outlined,
          ),
        );
      case HomeRole.departmentStaff:
        return _HomeRoleConfig(
          roleLabel: 'Cán bộ sở/ngành',
          title: 'FPT Schools',
          subtitle: 'Cán bộ Sở GD & ĐT',
          avatarInitials: 'SG',
          avatarIcon: Icons.account_balance_outlined,
          logoOnlyTitle: true,
          showHeaderBadge: true,
          filters: const [],
          features: const [
            _FeatureItem(
              label: 'Gửi thông báo Cán bộ Sở GD',
              icon: Icons.send_outlined,
            ),
            _FeatureItem(
              label: 'Gửi thông báo PGD, Trường',
              icon: Icons.mark_email_read_outlined,
            ),
            _FeatureItem(
              label: 'Lịch sử gửi thông báo',
              icon: Icons.history_outlined,
            ),
            _FeatureItem(label: 'Trang tin điện tử', icon: Icons.language),
            _FeatureItem(
              label: 'Hộp thư thông báo',
              icon: Icons.inbox_outlined,
              badge: 3,
            ),
            _FeatureItem(
              label: 'Quản lý cuộc họp',
              icon: Icons.groups_outlined,
            ),
          ],
          news: const [
            _NewsItem(
              title:
                  'Chỉ thị mới nhất về việc tăng cường công tác đảm bảo an...',
              date: '12/10/2023 - 08:30',
              style: _NewsVisualStyle.school,
            ),
            _NewsItem(
              title: 'Hội nghị giao ban ngành Giáo dục năm học 2022-2023',
              date: '10/10/2023 - 14:00',
              style: _NewsVisualStyle.meeting,
            ),
          ],
        );
    }
  }
}

class _FeatureItem {
  const _FeatureItem({required this.label, required this.icon, this.badge});

  final String label;
  final IconData icon;
  final int? badge;
}

class _NoticeItem {
  const _NoticeItem({
    required this.title,
    required this.description,
    required this.icon,
  });

  final String title;
  final String description;
  final IconData icon;
}

enum _NewsVisualStyle { school, meeting }

class _NewsItem {
  const _NewsItem({
    required this.title,
    required this.date,
    required this.style,
  });

  final String title;
  final String date;
  final _NewsVisualStyle style;
}

class _HomeHeader extends StatelessWidget {
  const _HomeHeader({required this.config});

  final _HomeRoleConfig config;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        _AvatarBadge(config: config),
        const SizedBox(width: 10),
        Expanded(
          child: config.logoOnlyTitle
              ? Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Image.asset(
                      'assets/images/fpt_schools_logo.jpg',
                      width: 96,
                      height: 24,
                      fit: BoxFit.contain,
                      alignment: Alignment.centerLeft,
                      semanticLabel: 'FPT Schools',
                    ),
                    if (config.subtitle.isNotEmpty) ...[
                      const SizedBox(height: 2),
                      Text(
                        config.subtitle,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: FptMobileColors.text,
                          fontSize: 11,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ],
                )
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      config.title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        fontSize: 15,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      config.subtitle,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: FptMobileColors.text,
                        fontSize: 11,
                      ),
                    ),
                  ],
                ),
        ),
        Stack(
          clipBehavior: Clip.none,
          children: [
            Icon(
              config.showHeaderBadge
                  ? Icons.notifications_none_rounded
                  : Icons.school_outlined,
              color: FptMobileColors.burntOrange,
              size: 23,
            ),
            if (config.showHeaderBadge)
              Positioned(
                top: -3,
                right: -1,
                child: Container(
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(
                    color: FptMobileColors.danger,
                    shape: BoxShape.circle,
                    border: Border.all(color: Colors.white, width: 1.5),
                  ),
                ),
              ),
          ],
        ),
      ],
    );
  }
}

class _AvatarBadge extends StatelessWidget {
  const _AvatarBadge({required this.config});

  final _HomeRoleConfig config;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 36,
      height: 36,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF6ED1D8), Color(0xFF243B55)],
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withAlpha(24),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Center(
        child: Icon(config.avatarIcon, color: Colors.white, size: 19),
      ),
    );
  }
}

class _FilterPills extends StatelessWidget {
  const _FilterPills({required this.filters});

  final List<String> filters;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 32,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 14),
        itemBuilder: (context, index) {
          final selected = index == 0;
          return Container(
            alignment: Alignment.center,
            padding: const EdgeInsets.symmetric(horizontal: 14),
            decoration: BoxDecoration(
              color: selected
                  ? FptMobileColors.softPeach
                  : const Color(0xFFF4F2F7),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(
                color: selected ? const Color(0xFFFFC39F) : Colors.transparent,
              ),
            ),
            child: Text(
              filters[index],
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                color: selected
                    ? FptMobileColors.burntOrange
                    : FptMobileColors.text,
                fontSize: 12,
                fontWeight: FontWeight.w800,
              ),
            ),
          );
        },
        separatorBuilder: (context, index) => const SizedBox(width: 10),
        itemCount: filters.length,
      ),
    );
  }
}

class _FeatureTile extends StatelessWidget {
  const _FeatureTile({required this.feature});

  final _FeatureItem feature;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Stack(
          clipBehavior: Clip.none,
          children: [
            Container(
              width: 46,
              height: 46,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: const Color(0xFFF4EFF5)),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withAlpha(8),
                    blurRadius: 8,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: Icon(
                feature.icon,
                color: FptMobileColors.burntOrange,
                size: 22,
              ),
            ),
            if (feature.badge != null)
              Positioned(
                top: -5,
                right: -5,
                child: _CountBadge(value: feature.badge!),
              ),
          ],
        ),
        const SizedBox(height: 8),
        Expanded(
          child: Text(
            feature.label,
            textAlign: TextAlign.center,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              fontSize: 11,
              height: 1.18,
              color: const Color(0xFF77777D),
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
      ],
    );
  }
}

class _CountBadge extends StatelessWidget {
  const _CountBadge({required this.value});

  final int value;

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minWidth: 16, minHeight: 16),
      padding: const EdgeInsets.symmetric(horizontal: 4),
      decoration: const BoxDecoration(
        color: FptMobileColors.danger,
        shape: BoxShape.circle,
      ),
      alignment: Alignment.center,
      child: Text(
        '$value',
        style: const TextStyle(
          color: Colors.white,
          fontSize: 9,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _PageDots extends StatelessWidget {
  const _PageDots({required this.activeIndex});

  final int activeIndex;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: List.generate(2, (index) {
        final active = index == activeIndex;
        return AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          width: active ? 6 : 5,
          height: active ? 6 : 5,
          margin: const EdgeInsets.symmetric(horizontal: 3),
          decoration: BoxDecoration(
            color: active
                ? FptMobileColors.burntOrange
                : const Color(0xFFE0DDE6),
            shape: BoxShape.circle,
          ),
        );
      }),
    );
  }
}

class _NoticeCard extends StatelessWidget {
  const _NoticeCard({required this.notice});

  final _NoticeItem notice;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 11),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFEDE8F0)),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF7B4D20).withAlpha(16),
            blurRadius: 14,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Container(
            width: 42,
            height: 42,
            decoration: BoxDecoration(
              color: const Color(0xFFEDE8EA),
              borderRadius: BorderRadius.circular(7),
            ),
            child: Icon(
              notice.icon,
              color: FptMobileColors.burntOrange,
              size: 22,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  notice.title,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: FptMobileColors.text,
                    fontSize: 13,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  notice.description,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: FptMobileColors.mutedText,
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
          const Icon(
            Icons.chevron_right,
            color: FptMobileColors.text,
            size: 18,
          ),
        ],
      ),
    );
  }
}

class _NewsSection extends StatelessWidget {
  const _NewsSection({required this.news});

  final List<_NewsItem> news;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(right: 14),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  'Tin nổi bật',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
              TextButton(
                onPressed: () {},
                style: TextButton.styleFrom(
                  foregroundColor: FptMobileColors.burntOrange,
                  minimumSize: const Size(0, 30),
                  padding: const EdgeInsets.symmetric(horizontal: 6),
                  textStyle: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                child: const Text('Xem thêm'),
              ),
            ],
          ),
        ),
        const SizedBox(height: 8),
        SizedBox(
          height: 170,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.only(right: 14),
            itemBuilder: (context, index) {
              return _NewsCard(item: news[index]);
            },
            separatorBuilder: (context, index) => const SizedBox(width: 12),
            itemCount: news.length,
          ),
        ),
      ],
    );
  }
}

class _NewsCard extends StatelessWidget {
  const _NewsCard({required this.item});

  final _NewsItem item;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 186,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFEFE7E2)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withAlpha(15),
            blurRadius: 10,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(height: 84, child: _NewsVisual(style: item.style)),
          Padding(
            padding: const EdgeInsets.fromLTRB(10, 8, 10, 0),
            child: Text(
              item.title,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                color: FptMobileColors.text,
                fontSize: 12,
                height: 1.28,
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
          const Spacer(),
          Padding(
            padding: const EdgeInsets.fromLTRB(10, 0, 10, 9),
            child: Text(
              item.date,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                color: FptMobileColors.burntOrange,
                fontSize: 10,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _NewsVisual extends StatelessWidget {
  const _NewsVisual({required this.style});

  final _NewsVisualStyle style;

  @override
  Widget build(BuildContext context) {
    if (style == _NewsVisualStyle.meeting) {
      return Container(
        color: const Color(0xFF23272F),
        child: Stack(
          fit: StackFit.expand,
          children: [
            Positioned.fill(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [
                      const Color(0xFF2B3442),
                      FptMobileColors.burntOrange.withAlpha(150),
                    ],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                ),
              ),
            ),
            Positioned(
              left: 18,
              right: 18,
              bottom: 18,
              child: Container(
                height: 30,
                decoration: BoxDecoration(
                  color: Colors.white.withAlpha(235),
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
            ),
            Positioned(
              left: 30,
              bottom: 29,
              child: Row(
                children: List.generate(
                  4,
                  (index) => Container(
                    width: 18,
                    height: 18,
                    margin: const EdgeInsets.only(right: 9),
                    decoration: const BoxDecoration(
                      color: Color(0xFF3F4754),
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      );
    }

    return Container(
      color: const Color(0xFFBFEAF6),
      child: Stack(
        fit: StackFit.expand,
        children: [
          Positioned.fill(
            child: DecoratedBox(
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  colors: [Color(0xFFDBF7FF), Color(0xFFB6E8CE)],
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                ),
              ),
            ),
          ),
          Positioned(
            left: 38,
            right: 38,
            top: 19,
            child: Container(
              height: 40,
              decoration: BoxDecoration(
                color: const Color(0xFFFFD592),
                borderRadius: BorderRadius.circular(3),
                border: Border.all(color: const Color(0xFFE7A85C)),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: List.generate(
                  3,
                  (_) => Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: List.generate(
                      5,
                      (_) => Container(
                        width: 9,
                        height: 4,
                        color: const Color(0xFF5EAAC0),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            child: Container(height: 25, color: const Color(0xFF69BD78)),
          ),
          Positioned(left: 14, bottom: 12, child: _Tree(size: 24)),
          Positioned(right: 18, bottom: 13, child: _Tree(size: 22)),
          Positioned(
            left: 50,
            right: 50,
            bottom: 11,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: List.generate(
                6,
                (index) => Container(
                  width: 3,
                  height: 18,
                  color: index.isEven
                      ? FptMobileColors.orange
                      : const Color(0xFF0C7CC1),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Tree extends StatelessWidget {
  const _Tree({required this.size});

  final double size;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: size,
      height: size,
      child: Stack(
        alignment: Alignment.bottomCenter,
        children: [
          Container(
            width: size * 0.16,
            height: size * 0.55,
            color: const Color(0xFF8D5F2C),
          ),
          Positioned(
            bottom: size * 0.28,
            child: Container(
              width: size,
              height: size * 0.68,
              decoration: const BoxDecoration(
                color: Color(0xFF3DA66C),
                shape: BoxShape.circle,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _HomeBottomNavigationBar extends StatelessWidget {
  const _HomeBottomNavigationBar();

  @override
  Widget build(BuildContext context) {
    const items = [
      _NavItem(
        label: 'Trang chủ',
        icon: Icons.home_outlined,
        activeIcon: Icons.home_rounded,
      ),
      _NavItem(label: 'Chat', icon: Icons.chat_bubble_outline),
      _NavItem(label: 'AI Assistant', icon: Icons.business_center_outlined),
      _NavItem(label: 'Thông báo', icon: Icons.notifications_none),
      _NavItem(label: 'Menu', icon: Icons.menu),
    ];

    return Container(
      height: 66,
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border.all(color: FptMobileColors.line),
        borderRadius: const BorderRadius.vertical(top: Radius.circular(10)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withAlpha(10),
            blurRadius: 12,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: SafeArea(
        top: false,
        child: Row(
          children: List.generate(items.length, (index) {
            return Expanded(
              child: _BottomNavButton(item: items[index], active: index == 0),
            );
          }),
        ),
      ),
    );
  }
}

class _NavItem {
  const _NavItem({
    required this.label,
    required this.icon,
    IconData? activeIcon,
  }) : activeIcon = activeIcon ?? icon;

  final String label;
  final IconData icon;
  final IconData activeIcon;
}

class _BottomNavButton extends StatelessWidget {
  const _BottomNavButton({required this.item, required this.active});

  final _NavItem item;
  final bool active;

  @override
  Widget build(BuildContext context) {
    final color = active
        ? FptMobileColors.burntOrange
        : const Color(0xFF686D76);

    return Center(
      child: Container(
        width: 52,
        height: 54,
        decoration: BoxDecoration(
          color: active ? FptMobileColors.softPeach : Colors.transparent,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(active ? item.activeIcon : item.icon, color: color, size: 21),
            const SizedBox(height: 3),
            Text(
              item.label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                color: color,
                fontSize: 10,
                fontWeight: active ? FontWeight.w800 : FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
