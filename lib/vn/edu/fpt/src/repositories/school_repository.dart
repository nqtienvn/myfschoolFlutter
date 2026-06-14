import '../api/client/fake_api.dart';
import '../api/dto/grade_dto.dart';
import '../api/dto/student_dto.dart';
import '../api/exception/student_not_found_exception.dart';
import '../models/models.dart';

typedef JsonCollectionLoader =
    Future<List<Map<String, dynamic>>> Function(String collectionName);

class SchoolRepository {
  final JsonCollectionLoader _collectionLoader;
  final FakeApiClient? _apiClient;

  const SchoolRepository.fromJsonLoader(JsonCollectionLoader loadCollection)
    : _collectionLoader = loadCollection,
      _apiClient = null;

  const SchoolRepository.fromApiClient({
    required JsonCollectionLoader loadCollection,
    required FakeApiClient apiClient,
  }) : _collectionLoader = loadCollection,
       _apiClient = apiClient;

  factory SchoolRepository.demo() {
    return const SchoolRepository.fromJsonLoader(_loadDemoCollection);
  }

  Future<Student> loadStudent(int studentId) async {
    final items = await _collectionLoader('students');
    final matches = items.where((item) => item['id'] == studentId);
    if (matches.isEmpty) {
      throw StudentNotFoundException(studentId);
    }
    return StudentDto.fromJson(matches.first).toDomain();
  }

  Future<List<Grade>> loadGrades(int studentId) async {
    final items = await _collectionLoader('grades');
    return items
        .where((item) => item['studentId'] == studentId)
        .map((item) => GradeDto.fromJson(item).toDomain())
        .toList();
  }

  Future<List<Grade>> getGrades({required int studentId}) async {
    final client = _apiClient;
    if (client == null) {
      return loadGrades(studentId);
    }

    final response = await client.getGradesResponse(studentId: studentId);
    final data = response['data'] as Map<String, dynamic>;
    final items = data['items'] as List<dynamic>;

    return items
        .map(
          (item) => GradeDto.fromJson(item as Map<String, dynamic>).toDomain(),
        )
        .toList();
  }

  Future<AttendanceStats> loadAttendanceStats(int studentId) async {
    final items = await _collectionLoader('attendance');
    final statuses = items
        .where((item) => item['studentId'] == studentId)
        .map((item) => item['status'] as String)
        .toList();

    return AttendanceStats.fromStatuses(statuses);
  }

  Future<int> loadMissingHomeworkCount(int studentId) async {
    final items = await _collectionLoader('assignments');
    return items
        .where(
          (item) =>
              item['studentId'] == studentId && item['status'] == 'MISSING',
        )
        .length;
  }

  Future<int> loadUnreadAnnouncementCount(int studentId) async {
    final items = await _collectionLoader('announcements');
    return items
        .where(
          (item) => item['studentId'] == studentId && item['isRead'] == false,
        )
        .length;
  }

  Stream<AppNotification> watchNotifications() async* {
    await Future<void>.delayed(const Duration(milliseconds: 500));
    yield AppNotification(
      id: 1,
      title: 'Diem moi',
      message: 'An vua co diem Toan 8.5.',
      createdAt: DateTime.now(),
    );
    await Future<void>.delayed(const Duration(seconds: 5));
    yield AppNotification(
      id: 2,
      title: 'Bai tap sap den han',
      message: 'Bai tap Van can nop truoc 20:00.',
      createdAt: DateTime.now(),
    );
    await Future<void>.delayed(const Duration(seconds: 5));
    yield AppNotification(
      id: 3,
      title: 'Thong bao lop',
      message: 'Lop 10A1 doi phong hoc tiet 3.',
      createdAt: DateTime.now(),
    );
  }
}

const _demoCollections = <String, List<Map<String, Object?>>>{
  'students': [
    {
      'id': 10,
      'code': 'STU-001',
      'fullName': 'Nguyen Minh An',
      'status': 'ACTIVE',
      'dateOfBirth': '2010-09-01',
      'avatarUrl': null,
    },
  ],
  'grades': [
    {
      'studentId': 10,
      'id': 101,
      'subjectName': 'Toan',
      'value': 8.5,
      'weight': 1,
      'createdAt': '2026-06-01T08:30:00Z',
      'comment': null,
    },
    {
      'studentId': 10,
      'id': 102,
      'subjectName': 'Van',
      'value': 7.5,
      'weight': 2,
      'createdAt': '2026-06-03T08:30:00Z',
      'comment': 'Can on them bai hinh',
    },
  ],
  'attendance': [
    {'studentId': 10, 'date': '2026-06-01', 'status': 'PRESENT'},
    {'studentId': 10, 'date': '2026-06-02', 'status': 'PRESENT'},
    {'studentId': 10, 'date': '2026-06-03', 'status': 'LATE'},
    {'studentId': 10, 'date': '2026-06-04', 'status': 'ABSENT'},
  ],
  'assignments': [
    {'id': 51, 'studentId': 10, 'title': 'Bai tap Toan', 'status': 'SUBMITTED'},
    {'id': 52, 'studentId': 10, 'title': 'Bai tap Van', 'status': 'MISSING'},
  ],
  'announcements': [
    {'id': 1, 'studentId': 10, 'title': 'Hop phu huynh', 'isRead': false},
    {'id': 2, 'studentId': 10, 'title': 'Lich kiem tra', 'isRead': true},
  ],
};

Future<List<Map<String, dynamic>>> _loadDemoCollection(
  String collectionName,
) async {
  final items = _demoCollections[collectionName];
  if (items == null) {
    throw ArgumentError.value(collectionName, 'collectionName');
  }
  return items.map((item) => Map<String, dynamic>.from(item)).toList();
}
