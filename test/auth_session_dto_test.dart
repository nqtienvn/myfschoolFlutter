import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/dto/auth_session_dto.dart';

void main() {
  test('parent login parses every linked student with profile details', () {
    final session = AuthSessionDto.fromJson({
      'token': 'token',
      'tokenType': 'Bearer',
      'expiresIn': 3600,
      'user': {
        'id': 1,
        'name': 'Phụ huynh',
        'phone': '0900000000',
        'role': 'PARENT',
        'status': 'ACTIVE',
        'parentProfile': {
          'id': 2,
          'children': [
            {
              'id': 10,
              'name': 'Học sinh A',
              'studentCode': 'HS001',
              'className': '10A1',
              'classId': 3,
              'schoolName': 'FPT Schools',
              'academicYearName': '2026-2027',
              'homeroomTeacherName': 'Cô Nguyễn Thu Hà',
              'homeroomTeacherPhone': '0901234567',
              'dateOfBirth': '2010-01-02',
              'gender': 'FEMALE',
              'address': 'Hà Nội',
              'status': 'ACTIVE',
            },
            {
              'id': 11,
              'name': 'Học sinh B',
              'studentCode': 'HS002',
              'status': 'ACTIVE',
            },
          ],
        },
      },
    }).toDomain();

    expect(session.children, hasLength(2));
    expect(session.children.first.name, 'Học sinh A');
    expect(session.children.first.className, '10A1');
    expect(session.children.first.homeroomTeacherName, 'Cô Nguyễn Thu Hà');
    expect(session.children.first.homeroomTeacherPhone, '0901234567');
    expect(session.children.first.dateOfBirth, '2010-01-02');
    expect(session.children.last.studentCode, 'HS002');
  });
}
