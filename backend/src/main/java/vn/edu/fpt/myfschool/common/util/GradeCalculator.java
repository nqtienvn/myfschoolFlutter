package vn.edu.fpt.myfschool.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class GradeCalculator {
    private GradeCalculator() {}

    public static BigDecimal calculateAverage(BigDecimal oral, BigDecimal quiz15m,
                                               BigDecimal midTerm, BigDecimal finalScore) {
        BigDecimal o = oral != null ? oral : BigDecimal.ZERO;
        BigDecimal q = quiz15m != null ? quiz15m : BigDecimal.ZERO;
        BigDecimal m = midTerm != null ? midTerm : BigDecimal.ZERO;
        BigDecimal f = finalScore != null ? finalScore : BigDecimal.ZERO;
        BigDecimal weighted = o.add(q.multiply(BigDecimal.valueOf(2)))
                .add(m.multiply(BigDecimal.valueOf(3)))
                .add(f.multiply(BigDecimal.valueOf(4)));
        return weighted.divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
    }

    public static String getAcademicAbility(BigDecimal gpa) {
        if (gpa == null) return null;
        if (gpa.compareTo(BigDecimal.valueOf(8.0)) >= 0) return "Giỏi";
        if (gpa.compareTo(BigDecimal.valueOf(6.5)) >= 0) return "Khá";
        if (gpa.compareTo(BigDecimal.valueOf(5.0)) >= 0) return "Trung bình";
        return "Yếu";
    }

    public static String getConduct(double attendanceRate) {
        if (attendanceRate >= 90) return "Tốt";
        if (attendanceRate >= 75) return "Khá";
        if (attendanceRate >= 60) return "Trung bình";
        return "Yếu";
    }
}
