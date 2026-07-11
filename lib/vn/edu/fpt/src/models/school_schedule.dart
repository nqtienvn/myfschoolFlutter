class SchoolSchedule {
  const SchoolSchedule({
    required this.ownerId,
    required this.ownerName,
    required this.semesterId,
    required this.semesterName,
    required this.days,
    this.timetableId,
    this.timetableVersion,
  });

  final int ownerId;
  final String ownerName;
  final int semesterId;
  final String semesterName;
  final int? timetableId;
  final int? timetableVersion;
  final List<ScheduleDay> days;

  ScheduleDay day(int dayOfWeek) => days.firstWhere(
    (item) => item.dayOfWeek == dayOfWeek,
    orElse: () => ScheduleDay(
      dayOfWeek: dayOfWeek,
      dayOfWeekName: '',
      morningLessons: const [],
      afternoonLessons: const [],
    ),
  );
}

class ScheduleDay {
  const ScheduleDay({
    required this.dayOfWeek,
    required this.dayOfWeekName,
    required this.morningLessons,
    required this.afternoonLessons,
  });

  final int dayOfWeek;
  final String dayOfWeekName;
  final List<ScheduleLesson> morningLessons;
  final List<ScheduleLesson> afternoonLessons;
}

class ScheduleLesson {
  const ScheduleLesson({
    required this.id,
    required this.classId,
    required this.className,
    required this.subjectName,
    required this.subjectCode,
    required this.teacherName,
    required this.dayOfWeek,
    required this.period,
    required this.room,
    required this.shift,
  });

  final int id;
  final int classId;
  final String className;
  final String subjectName;
  final String subjectCode;
  final String teacherName;
  final int dayOfWeek;
  final int period;
  final String room;
  final String shift;
}
