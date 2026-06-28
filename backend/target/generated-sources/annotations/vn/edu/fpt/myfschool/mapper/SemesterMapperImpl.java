package vn.edu.fpt.myfschool.mapper;

import java.time.LocalDate;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import vn.edu.fpt.myfschool.common.dto.SemesterDto;
import vn.edu.fpt.myfschool.entity.Semester;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-28T20:44:02+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class SemesterMapperImpl implements SemesterMapper {

    @Override
    public SemesterDto toDto(Semester semester) {
        if ( semester == null ) {
            return null;
        }

        Long id = null;
        String name = null;
        String academicYear = null;
        LocalDate startDate = null;
        LocalDate endDate = null;
        Boolean isCurrent = null;

        id = semester.getId();
        name = semester.getName();
        academicYear = semester.getAcademicYear();
        startDate = semester.getStartDate();
        endDate = semester.getEndDate();
        isCurrent = semester.getIsCurrent();

        SemesterDto semesterDto = new SemesterDto( id, name, academicYear, startDate, endDate, isCurrent );

        return semesterDto;
    }
}
