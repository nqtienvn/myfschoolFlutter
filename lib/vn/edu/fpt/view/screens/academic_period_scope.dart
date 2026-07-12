import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';

@immutable
class AcademicPeriod {
  const AcademicPeriod({
    required this.academicYearId,
    required this.academicYearName,
    required this.semesterId,
    required this.semesterName,
    this.isCurrent = false,
  });

  final int academicYearId;
  final String academicYearName;
  final int semesterId;
  final String semesterName;
  final bool isCurrent;

  String get label => '$academicYearName · $semesterName';
}

class AcademicPeriodController extends ChangeNotifier {
  AcademicPeriodController({required this.token});

  final String token;
  bool isLoading = true;
  List<AcademicPeriod> periods = const [];
  AcademicPeriod? selected;

  Future<void> load() async {
    try {
      final data = await BackendApiClient().getData(
        '/api/academic-years/available',
        token: token,
      );
      final rows = (data as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>();
      final loaded = <AcademicPeriod>[];
      for (final year in rows) {
        final yearId = year['id'] as int?;
        if (yearId == null) continue;
        for (final semester
            in (year['semesters'] as List<dynamic>? ?? const [])
                .whereType<Map<String, dynamic>>()) {
          final semesterId = semester['id'] as int?;
          if (semesterId == null) continue;
          loaded.add(
            AcademicPeriod(
              academicYearId: yearId,
              academicYearName: year['name'] as String? ?? '',
              semesterId: semesterId,
              semesterName: semester['name'] as String? ?? '',
              isCurrent: semester['isCurrent'] as bool? ?? false,
            ),
          );
        }
      }
      periods = loaded;
      AcademicPeriod? current;
      for (final period in loaded) {
        if (period.isCurrent) {
          current = period;
          break;
        }
      }
      selected = current ?? (loaded.isEmpty ? null : loaded.first);
    } catch (_) {
      periods = const [];
      selected = null;
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  void select(AcademicPeriod period) {
    if (selected == period) return;
    selected = period;
    notifyListeners();
  }
}

class AcademicPeriodScope extends InheritedNotifier<AcademicPeriodController> {
  const AcademicPeriodScope({
    super.key,
    required AcademicPeriodController controller,
    required super.child,
  }) : super(notifier: controller);

  static AcademicPeriodController? maybeOf(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<AcademicPeriodScope>()?.notifier;
}
