package vn.edu.fpt.myfschool.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Quy tắc xếp mức theo Thông tư 22/2021/TT-BGDĐT và ngưỡng cảnh báo nội bộ.
 * GPA chỉ là số tham khảo; xếp mức học tập luôn dựa trên kết quả từng môn.
 */
public final class ResultClassificationPolicy {
    private static final List<String> LEVELS = List.of("Tốt", "Khá", "Đạt", "Chưa đạt");

    private ResultClassificationPolicy() {}

    public static String academicAbility(List<BigDecimal> subjectAverages, int failedCommentSubjects) {
        if (subjectAverages == null || subjectAverages.isEmpty()) return "Chưa đạt";
        String raw = baseAcademicAbility(subjectAverages, failedCommentSubjects);
        int rawLevel = LEVELS.indexOf(raw);
        if (rawLevel < 2) return raw;

        // Khoản điều chỉnh của Thông tư 22: nếu duy nhất một môn làm kết quả tụt từ
        // hai mức trở lên thì nâng kết quả chung lên đúng một mức liền kề.
        for (int index = 0; index < subjectAverages.size(); index++) {
            List<BigDecimal> withoutOneLowResult = new ArrayList<>(subjectAverages);
            withoutOneLowResult.set(index, BigDecimal.TEN);
            if (isAtLeastTwoLevelsBetter(baseAcademicAbility(
                    withoutOneLowResult, failedCommentSubjects), rawLevel)) {
                return LEVELS.get(rawLevel - 1);
            }
        }
        if (failedCommentSubjects > 0 && isAtLeastTwoLevelsBetter(
                baseAcademicAbility(subjectAverages, failedCommentSubjects - 1), rawLevel)) {
            return LEVELS.get(rawLevel - 1);
        }
        return raw;
    }

    private static String baseAcademicAbility(List<BigDecimal> subjectAverages, int failedCommentSubjects) {
        long atLeastEight = countAtLeast(subjectAverages, 8.0);
        long atLeastSixFive = countAtLeast(subjectAverages, 6.5);
        long atLeastFive = countAtLeast(subjectAverages, 5.0);
        boolean allAtLeastSixFive = subjectAverages.stream().allMatch(value -> atLeast(value, 6.5));
        boolean allAtLeastFive = subjectAverages.stream().allMatch(value -> atLeast(value, 5.0));
        boolean noneBelowThreeFive = subjectAverages.stream().allMatch(value -> atLeast(value, 3.5));

        if (failedCommentSubjects == 0 && allAtLeastSixFive && atLeastEight >= 6) return "Tốt";
        if (failedCommentSubjects == 0 && allAtLeastFive && atLeastSixFive >= 6) return "Khá";
        if (failedCommentSubjects <= 1 && atLeastFive >= 6 && noneBelowThreeFive) return "Đạt";
        return "Chưa đạt";
    }

    private static boolean isAtLeastTwoLevelsBetter(String candidate, int rawLevel) {
        int candidateLevel = LEVELS.indexOf(candidate);
        return candidateLevel >= 0 && candidateLevel <= rawLevel - 2;
    }

    public static String suggestedConduct(long violationCount, long absentWithoutLeave) {
        if (violationCount == 0 && absentWithoutLeave <= 2) return "Tốt";
        if (violationCount <= 1 && absentWithoutLeave <= 4) return "Khá";
        if (violationCount <= 2 && absentWithoutLeave <= 9) return "Đạt";
        return "Chưa đạt";
    }

    /** Ma trận kết quả rèn luyện cả năm tại khoản 2 Điều 8 Thông tư 22. */
    public static String annualConduct(String semester1, String semester2) {
        String first = normalizeConduct(semester1);
        String second = normalizeConduct(semester2);
        int firstLevel = LEVELS.indexOf(first);
        int secondLevel = LEVELS.indexOf(second);
        if (secondLevel == 0 && firstLevel <= 1) return "Tốt";
        if ((secondLevel == 1 && firstLevel <= 2)
                || (secondLevel == 2 && firstLevel == 0)
                || (secondLevel == 0 && firstLevel >= 2)) return "Khá";
        if ((secondLevel == 2 && firstLevel >= 1)
                || (secondLevel == 1 && firstLevel == 3)) return "Đạt";
        return "Chưa đạt";
    }

    public static String normalizeConduct(String value) {
        if (value == null || value.isBlank()) return "Chưa đạt";
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "tốt", "tot" -> "Tốt";
            case "khá", "kha" -> "Khá";
            case "trung bình", "trung binh", "tb", "đạt", "dat" -> "Đạt";
            default -> "Chưa đạt";
        };
    }

    private static long countAtLeast(List<BigDecimal> values, double threshold) {
        return values.stream().filter(value -> atLeast(value, threshold)).count();
    }

    private static boolean atLeast(BigDecimal value, double threshold) {
        return value != null && value.compareTo(BigDecimal.valueOf(threshold)) >= 0;
    }
}
