import 'package:flutter_test/flutter_test.dart';

import '../bin/school_cli.dart';

void main() {
  test('school cli prints all main functions', () async {
    final output = await buildSchoolCliReport();

    expect(output, contains('MYFSCHOOL CLI CHECK'));
    expect(output, contains('Student: Nguyen Minh An (STU-001)'));
    expect(output, contains('Grades: 2'));
    expect(output, contains('Attendance sessions: 4'));
    expect(output, contains('Missing homework: 1'));
    expect(output, contains('Unread announcements: 1'));
    expect(output, contains('Average grade: 8.00'));
    expect(output, contains('Weighted average grade: 7.83'));
    expect(output, contains('Needs attention: true'));
    expect(output, contains('First notification: Diem moi'));
    expect(output, contains('OK: CLI functions printed successfully'));
  });
}
