package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import vn.edu.fpt.myfschool.service.ResultClassificationPolicy;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultClassificationPolicyTest {

    @Test
    void academicAbility_uses_subject_thresholds_insteadOfOverallGpa() {
        assertEquals("Tốt", ResultClassificationPolicy.academicAbility(
                scores(9, 8.5, 8, 8, 8, 8, 7, 6.5), 0));
        assertEquals("Khá", ResultClassificationPolicy.academicAbility(
                scores(8, 7, 7, 6.5, 6.5, 6.5, 5, 5), 0));
        assertEquals("Đạt", ResultClassificationPolicy.academicAbility(
                scores(7, 6, 5.5, 5, 5, 5, 4, 3.5), 1));
        // Một môn duy nhất kéo kết quả xuống từ hai mức được điều chỉnh lên một mức.
        assertEquals("Đạt", ResultClassificationPolicy.academicAbility(
                scores(10, 10, 10, 10, 10, 10, 3.4), 0));
        assertEquals("Khá", ResultClassificationPolicy.academicAbility(
                scores(10, 10, 10, 10, 10, 10, 4), 0));
        assertEquals("Khá", ResultClassificationPolicy.academicAbility(
                scores(10, 10, 10, 10, 10, 10, 10), 1));
        assertEquals("Chưa đạt", ResultClassificationPolicy.academicAbility(
                scores(10, 10, 10, 10, 10, 10, 3.4, 3.0), 0));
    }

    @Test
    void conductThresholds_haveNoBoundaryGaps() {
        assertEquals("Tốt", ResultClassificationPolicy.suggestedConduct(2));
        assertEquals("Khá", ResultClassificationPolicy.suggestedConduct(4));
        assertEquals("Đạt", ResultClassificationPolicy.suggestedConduct(9));
        assertEquals("Chưa đạt", ResultClassificationPolicy.suggestedConduct(10));
    }

    @Test
    void annualConduct_matchesCircular22SecondSemesterMatrix() {
        assertEquals("Tốt", ResultClassificationPolicy.annualConduct("Khá", "Tốt"));
        assertEquals("Khá", ResultClassificationPolicy.annualConduct("Trung bình", "Khá"));
        assertEquals("Khá", ResultClassificationPolicy.annualConduct("Yếu", "Tốt"));
        assertEquals("Khá", ResultClassificationPolicy.annualConduct("Trung bình", "Tốt"));
        assertEquals("Đạt", ResultClassificationPolicy.annualConduct("Đạt", "Đạt"));
    }

    private List<BigDecimal> scores(double... values) {
        return java.util.Arrays.stream(values).mapToObj(BigDecimal::valueOf).toList();
    }
}
