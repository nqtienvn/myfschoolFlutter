import 'dart:convert';

import 'package:myfschoolse1913/vn/edu/fpt/src/api/exception/fake_api_exception.dart';

class FakeApiClient {
  final Duration delay;
  final bool shouldFail;

  const FakeApiClient({
    this.delay = const Duration(milliseconds: 500),
    this.shouldFail = false,
  });

  Future<Map<String, dynamic>> getGradesResponse({
    required int studentId,
  }) async {
    await Future<void>.delayed(delay);

    if (shouldFail) {
      throw const FakeApiException('Không thể tải điểm từ server giả lập.');
    }

    final rawJson =
        '''
    {
      "data": {
        "studentId": $studentId,
        "items": [
          {
            "id": 101,
            "subjectName": "Toán",
            "value": 8.5,
            "weight": 1,
            "createdAt": "2026-06-03T08:30:00Z",
            "comment": null
          },
          {
            "id": 102,
            "subjectName": "Văn",
            "value": 9,
            "weight": 2,
            "createdAt": "2026-06-04T08:30:00Z",
            "comment": "Bài viết tốt"
          }
        ]
      },
      "meta": null,
      "error": null
    }
    ''';

    return jsonDecode(rawJson) as Map<String, dynamic>;
  }
}
