import 'package:myfschoolse1913/vn/edu/fpt/packages/core/core.dart';
void main() {
  const user = User(
    id: 1,
    email: 'parent@edulink.test',
    fullName: 'Nguyễn Văn Bình',
    role: UserRole.guardian,
    active: true,
    phone: null,
  );

  final student = Student(
    id: 10,
    code: 'STU-001',
    fullName: 'Nguyễn Minh An',
    status: 'ACTIVE',
    dateOfBirth: DateTime(2010, 9, 1),
  );

  final grade = Grade(
    id: 100,
    subjectName: 'Toán',
    value: 8.5,
    weight: 1.0,
    createdAt: DateTime.now(),
    comment: null,
  );

    print(user.displayName);
    print(student.displayCode);
    print('${grade.subjectName}: ${grade.value} - ${grade.displayComment}');

}
