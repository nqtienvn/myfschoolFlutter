class ApiResponse<T> {
  final int code;
  final String message;
  final T? result;

  const ApiResponse({
    required this.code,
    required this.message,
    this.result,
  });

  factory ApiResponse.fromJson(
      Map<String, dynamic> json,
      T Function(Object? json)? fromJsonT,
      ) {
    final rawCode = json['code'];
    final rawMessage = json['message'];
    final rawResult = json['result'];

    if (rawCode is! int) {
      throw Exception('Field "code" must be int.');
    }

    if (rawMessage is! String) {
      throw Exception('Field "message" must be String.');
    }

    return ApiResponse<T>(
      code: rawCode,
      message: rawMessage,
      result: rawResult == null || fromJsonT == null
          ? null
          : fromJsonT(rawResult),
    );
  }
}
