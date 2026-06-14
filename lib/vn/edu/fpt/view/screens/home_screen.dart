import 'package:flutter/cupertino.dart';
import '../widgets/info_card.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsetsGeometry.all(16),
      children: const [
        Text(
          'Xin chào, phụ huynh của Nguyễn Minh An',
          style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
        ),
        SizedBox(height: 12),
        InfoCard(
          title: "AI Daily Summary",
          content:
          'Hôm nay Tiền đi học đầy đủ. Em còn 1 bài tập văn chưa nộp và có 2 thông báo mới chưa đọc',
        ),
        InfoCard(
          title: 'Tổng quan học sinh',
          content:
          'Điểm trung bình: 8.17\nTỷ lệ chuyên cần: 80%\nTrạng thái: Có nội dung cần chú ý',
        ),
        InfoCard(
          title: 'Học phí',
          content:
          'Học phí kì này còn thiếu của bạn là 28 củ nha',
        ),
        InfoCard(
          title: 'Gợi ý tiếp theo',
          content:
          'Sau này dữ liệu này sẽ đến từ StudentSummary và backend API, không hard-code trong UI.',
        ),
      ],
    );
  }
}