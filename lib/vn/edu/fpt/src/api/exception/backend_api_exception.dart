class BackendApiException implements Exception {
  final String message;
  final int? statusCode;

  const BackendApiException(this.message, {this.statusCode});

  @override
  String toString() => statusCode == null
      ? 'BackendApiException: $message'
      : 'BackendApiException($statusCode): $message';
}
