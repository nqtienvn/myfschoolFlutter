import '../api/dto/student_dto.dart';
import '../api/exception/student_not_found_exception.dart';
import '../models/models.dart';

typedef JsonCollectionLoader =
    Future<List<Map<String, dynamic>>> Function(String collectionName);

class SchoolRepository {
  final JsonCollectionLoader _collectionLoader;

  const SchoolRepository.fromJsonLoader(JsonCollectionLoader loadCollection)
    : _collectionLoader = loadCollection;

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

  Future<List<SubjectTranscript>> loadTranscript(int studentId) async {
    final rows = await _collectionLoader('transcripts');
    return rows
        .where((row) => row['studentId'] == studentId)
        .map(_parseTranscript)
        .toList();
  }

  SubjectTranscript _parseTranscript(Map<String, dynamic> row) {
    final columns = (row['columns'] as List).map((raw) {
      final item = raw as Map<String, dynamic>;
      return GradeColumn(
        id: item['id'] as int,
        code: item['code'] as String,
        name: item['name'] as String,
        weight: item['weight'] as int,
        entryRole: GradeEntryRole.values.byName(item['entryRole'] as String),
      );
    }).toList();
    final values = <int, double?>{};
    for (final entry in (row['scores'] as Map<String, dynamic>).entries) {
      values[int.parse(entry.key)] = (entry.value as num?)?.toDouble();
    }
    return SubjectTranscript(
      subjectName: row['subjectName'] as String,
      columns: columns,
      scores: values,
      average: (row['average'] as num?)?.toDouble(),
    );
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
    },
  ],
  'transcripts': [
    {
      'studentId': 10,
      'subjectName': 'Toan',
      'columns': [
        {
          'id': 1,
          'code': 'TX_1',
          'name': 'Thuong xuyen',
          'weight': 1,
          'entryRole': 'subjectTeacher',
        },
        {
          'id': 2,
          'code': 'GK_1',
          'name': 'Giua ky',
          'weight': 2,
          'entryRole': 'admin',
        },
      ],
      'scores': {'1': 8.0, '2': 8.3},
      'average': 8.2,
    },
  ],
  'attendance': [
    {'studentId': 10, 'date': '2026-06-01', 'status': 'PRESENT'},
    {'studentId': 10, 'date': '2026-06-02', 'status': 'PRESENT'},
    {'studentId': 10, 'date': '2026-06-03', 'status': 'PRESENT'},
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
