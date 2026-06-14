class AttendanceStats {
  final int presentCount; //so luong co mat
  final int absentCount; //so luong vang mat
  final int lateCount; //muon
  final int excusedCount; //buoi nghi co phep

  const AttendanceStats({
    required this.presentCount,
    required this.absentCount,
    this.lateCount = 0,
    this.excusedCount = 0,
  });

  const AttendanceStats.empty()
    : presentCount = 0,
      absentCount = 0,
      lateCount = 0,
      excusedCount = 0;

  int get totalSessions =>
      presentCount + absentCount + lateCount + excusedCount;

  double get attendanceRate {
    if (totalSessions == 0) return 0;
    return (presentCount + lateCount + excusedCount) / totalSessions;
  }

  factory AttendanceStats.fromStatuses(List<String> statuses) {
    //thuc hien logic truoc khi khoi tao --< co logic cai tao object luon
    var present = 0;
    var absent = 0;
    var late = 0;
    var excused = 0;

    for (final status in statuses) {
      switch (status) {
        case 'PRESENT':
          present++;
        case 'ABSENT':
          absent++;
        case 'LATE':
          late++;
        case 'EXCUSED':
          excused++;
      }
    }

    return AttendanceStats(
      presentCount: present,
      absentCount: absent,
      lateCount: late,
      excusedCount: excused,
    );
  }
}
