import '../../models/school_schedule.dart';
import '../exception/parse_exception.dart';

class SchoolScheduleDto {
  const SchoolScheduleDto(this.schedule);

  final SchoolSchedule schedule;

  factory SchoolScheduleDto.fromJson(Map<String, dynamic> json) {
    final rawDays = json['days'];
    if (rawDays is! List) {
      throw const ParseException('Thiếu danh sách ngày trong thời khóa biểu');
    }
    return SchoolScheduleDto(
      SchoolSchedule(
        ownerId: requireField<int>(json, 'classId'),
        ownerName: requireField<String>(json, 'className'),
        semesterId: requireField<int>(json, 'semesterId'),
        semesterName: requireField<String>(json, 'semesterName'),
        timetableId: json['timetableId'] as int?,
        timetableVersion: json['timetableVersion'] as int?,
        days: rawDays
            .whereType<Map<String, dynamic>>()
            .map(_parseDay)
            .toList(growable: false),
      ),
    );
  }

  SchoolSchedule toDomain() => schedule;

  static ScheduleDay _parseDay(Map<String, dynamic> json) => ScheduleDay(
    dayOfWeek: requireField<int>(json, 'dayOfWeek'),
    dayOfWeekName: requireField<String>(json, 'dayOfWeekName'),
    morningLessons: _parseLessons(json['morningSlots']),
    afternoonLessons: _parseLessons(json['afternoonSlots']),
  );

  static List<ScheduleLesson> _parseLessons(Object? value) {
    if (value is! List) return const [];
    return value
        .whereType<Map<String, dynamic>>()
        .map((json) {
          return ScheduleLesson(
            id: requireField<int>(json, 'id'),
            classId: requireField<int>(json, 'classId'),
            className: requireField<String>(json, 'className'),
            subjectName: requireField<String>(json, 'subjectName'),
            subjectCode: requireField<String>(json, 'subjectCode'),
            teacherName: requireField<String>(json, 'teacherName'),
            dayOfWeek: requireField<int>(json, 'dayOfWeek'),
            period: requireField<int>(json, 'period'),
            periodName: requireField<String>(json, 'periodName'),
            room: json['room'] is String ? json['room'] as String : '',
            shift: requireField<String>(json, 'shift'),
          );
        })
        .toList(growable: false);
  }
}
