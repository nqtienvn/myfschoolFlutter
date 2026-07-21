import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';

@immutable
class AcademicPeriod {
  const AcademicPeriod({
    required this.academicYearId,
    required this.academicYearName,
    required this.semesterId,
    required this.semesterName,
    required this.startDate,
    required this.endDate,
    this.isCurrent = false,
    this.academicYearStatus = '',
    this.semesterStatus = '',
  });

  final int academicYearId;
  final String academicYearName;
  final int semesterId;
  final String semesterName;
  final DateTime startDate;
  final DateTime endDate;
  final bool isCurrent;
  final String academicYearStatus;
  final String semesterStatus;

  String get label => '$academicYearName · $semesterName';
  bool get isActive =>
      academicYearStatus == 'ACTIVE' &&
      (semesterStatus == 'ACTIVE' || isCurrent);

  DateTime get referenceDate {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    if (!today.isBefore(startDate) && !today.isAfter(endDate)) return today;
    return startDate;
  }
}

class AcademicPeriodController extends ChangeNotifier {
  AcademicPeriodController({
    required this.token,
    this.studentId,
    BackendApiClient? backend,
  }) : _backend = backend ?? BackendApiClient();

  final String token;
  final BackendApiClient _backend;
  int? studentId;
  bool isLoading = true;
  List<AcademicPeriod> periods = const [];
  AcademicPeriod? selected;
  String? errorMessage;
  int _loadGeneration = 0;

  Future<void> load() async {
    final generation = ++_loadGeneration;
    final requestedStudentId = studentId;
    isLoading = true;
    errorMessage = null;
    notifyListeners();
    try {
      final data = await _backend.getData(
        '/api/academic-years/available',
        token: token,
        query: {'studentId': requestedStudentId?.toString()},
      );
      final rows = (data as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>();
      final loaded = <AcademicPeriod>[];
      for (final year in rows) {
        final yearId = year['id'] as int?;
        if (yearId == null) continue;
        final yearStatus = year['status'] as String? ?? '';
        for (final semester
            in (year['semesters'] as List<dynamic>? ?? const [])
                .whereType<Map<String, dynamic>>()) {
          final semesterId = semester['id'] as int?;
          final startDate = DateTime.tryParse(
            semester['startDate'] as String? ?? '',
          );
          final endDate = DateTime.tryParse(
            semester['endDate'] as String? ?? '',
          );
          if (semesterId == null || startDate == null || endDate == null) {
            continue;
          }
          loaded.add(
            AcademicPeriod(
              academicYearId: yearId,
              academicYearName: year['name'] as String? ?? '',
              semesterId: semesterId,
              semesterName: semester['name'] as String? ?? '',
              startDate: startDate,
              endDate: endDate,
              isCurrent: semester['isCurrent'] as bool? ?? false,
              academicYearStatus: yearStatus,
              semesterStatus: semester['status'] as String? ?? '',
            ),
          );
        }
      }
      AcademicPeriod? current;
      for (final period in loaded) {
        if (period.isCurrent && period.isActive) {
          current = period;
          break;
        }
      }
      current ??= loaded.cast<AcademicPeriod?>().firstWhere(
        (period) => period?.isActive == true,
        orElse: () => null,
      );
      current ??= loaded.cast<AcademicPeriod?>().firstWhere(
        (period) => period?.isCurrent == true,
        orElse: () => null,
      );
      if (!_isCurrentLoad(generation, requestedStudentId)) return;
      periods = loaded;
      selected = current ?? (loaded.isEmpty ? null : loaded.first);
    } catch (error) {
      if (!_isCurrentLoad(generation, requestedStudentId)) return;
      periods = const [];
      selected = null;
      errorMessage = error.toString().replaceAll('Exception: ', '');
    } finally {
      if (_isCurrentLoad(generation, requestedStudentId)) {
        isLoading = false;
        notifyListeners();
      }
    }
  }

  bool _isCurrentLoad(int generation, int? requestedStudentId) =>
      generation == _loadGeneration && studentId == requestedStudentId;

  void select(AcademicPeriod period) {
    if (!periods.contains(period) || selected == period) return;
    selected = period;
    notifyListeners();
  }

  bool selectByIds({required int academicYearId, required int semesterId}) {
    for (final period in periods) {
      if (period.academicYearId == academicYearId &&
          period.semesterId == semesterId) {
        select(period);
        return true;
      }
    }
    return false;
  }

  Future<void> setStudentId(int? value) async {
    if (studentId == value) return;
    studentId = value;
    _loadGeneration++;
    periods = const [];
    selected = null;
    notifyListeners();
    await load();
  }
}

class AcademicPeriodScope extends InheritedNotifier<AcademicPeriodController> {
  const AcademicPeriodScope({
    super.key,
    required AcademicPeriodController controller,
    required super.child,
  }) : super(notifier: controller);

  static AcademicPeriodController? maybeOf(BuildContext context) => context
      .dependOnInheritedWidgetOfExactType<AcademicPeriodScope>()
      ?.notifier;
}
