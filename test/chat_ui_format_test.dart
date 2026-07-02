import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/chat_detail_screen.dart';

void main() {
  test('chat status label maps delivery states to Vietnamese UI text', () {
    expect(chatStatusLabel('sending'), 'Đang gửi');
    expect(chatStatusLabel('sent'), 'Đã gửi');
    expect(chatStatusLabel('delivered'), 'Đã nhận');
    expect(chatStatusLabel('read'), 'Đã xem');
    expect(chatStatusLabel('failed'), 'Gửi lỗi');
  });
}
