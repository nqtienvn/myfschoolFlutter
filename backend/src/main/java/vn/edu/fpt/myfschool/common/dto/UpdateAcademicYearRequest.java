package vn.edu.fpt.myfschool.common.dto;
import java.time.LocalDate;

public record UpdateAcademicYearRequest(LocalDate startDate, LocalDate endDate) {
}
