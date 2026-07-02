import '../exception/parse_exception.dart';

class ApiResponse<T> {
  final bool success;
  final String message;
  final T? data;
  final String? timestamp;

  const ApiResponse({
    required this.success,
    required this.message,
    this.data,
    this.timestamp,
  });

  factory ApiResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Object? json)? fromJsonT,
  ) {
    final success = requireField<bool>(json, 'success');
    final message = requireField<String>(json, 'message');
    final rawData = json['data'];
    final timestamp = json['timestamp'];

    if (timestamp != null && timestamp is! String) {
      throw const ParseException('Field "timestamp" must be String.');
    }

    return ApiResponse<T>(
      success: success,
      message: message,
      data: rawData == null || fromJsonT == null ? null : fromJsonT(rawData),
      timestamp: timestamp,
    );
  }
}
