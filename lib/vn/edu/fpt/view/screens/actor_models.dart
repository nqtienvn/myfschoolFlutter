import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';

enum AppActor { parent, teacher, student }

extension AppActorInfo on AppActor {
  String get label {
    switch (this) {
      case AppActor.parent:
        return 'Phụ huynh';
      case AppActor.teacher:
        return 'Giáo viên';
      case AppActor.student:
        return 'Học sinh';
    }
  }

  String get dashboardTitle {
    switch (this) {
      case AppActor.parent:
        return 'Dashboard phụ huynh';
      case AppActor.teacher:
        return 'Dashboard giáo viên';
      case AppActor.student:
        return 'Dashboard học sinh';
    }
  }

  IconData get icon {
    switch (this) {
      case AppActor.parent:
        return Icons.family_restroom;
      case AppActor.teacher:
        return Icons.co_present;
      case AppActor.student:
        return Icons.school;
    }
  }

  Color get color {
    switch (this) {
      case AppActor.parent:
        return AppColors.fptOrange;
      case AppActor.teacher:
        return AppColors.blue;
      case AppActor.student:
        return AppColors.green;
    }
  }
}
