import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/dto/school_schedule_dto.dart';

void main() {
  test('parses class schedule and teacher assignment details', () {
    final dto = SchoolScheduleDto.fromJson({
      'classId': 12,
      'className': '12A',
      'semesterId': 1,
      'semesterName': 'HK I',
      'timetableId': 8,
      'timetableVersion': 2,
      'days': [
        {
          'dayOfWeek': 2,
          'dayOfWeekName': 'Thứ 2',
          'morningSlots': [
            {
              'id': 20,
              'classId': 12,
              'className': '12A',
              'subjectName': 'Toán',
              'subjectCode': 'TOAN12',
              'teacherName': 'Nguyễn Văn A',
              'dayOfWeek': 2,
              'period': 1,
              'room': '12A',
              'shift': 'MORNING',
            },
          ],
          'afternoonSlots': [],
        },
      ],
    });

    final schedule = dto.toDomain();
    final lesson = schedule.day(2).morningLessons.single;

    expect(schedule.ownerName, '12A');
    expect(schedule.timetableVersion, 2);
    expect(lesson.subjectName, 'Toán');
    expect(lesson.teacherName, 'Nguyễn Văn A');
    expect(lesson.room, '12A');
  });

  test('returns an empty day when backend omits that weekday', () {
    final schedule = SchoolScheduleDto.fromJson({
      'classId': 12,
      'className': '12A',
      'semesterId': 1,
      'semesterName': 'HK I',
      'timetableId': null,
      'timetableVersion': null,
      'days': <Map<String, dynamic>>[],
    }).toDomain();

    expect(schedule.day(6).morningLessons, isEmpty);
    expect(schedule.day(6).afternoonLessons, isEmpty);
  });
}
