package vn.edu.fpt.myfschool.mapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import vn.edu.fpt.myfschool.common.dto.ClassDto;
import vn.edu.fpt.myfschool.entity.SchoolClass;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-28T20:44:02+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class ClassMapperImpl implements ClassMapper {

    @Override
    public ClassDto toDto(SchoolClass cls) {
        if ( cls == null ) {
            return null;
        }

        Long id = null;
        String name = null;
        Integer gradeLevel = null;
        String academicYear = null;
        String schoolName = null;
        LocalDateTime createdAt = null;

        id = cls.getId();
        name = cls.getName();
        gradeLevel = cls.getGradeLevel();
        academicYear = cls.getAcademicYear();
        schoolName = cls.getSchoolName();
        createdAt = cls.getCreatedAt();

        Integer studentCount = cls.getStudents() != null ? cls.getStudents().size() : 0;

        ClassDto classDto = new ClassDto( id, name, gradeLevel, academicYear, schoolName, studentCount, createdAt );

        return classDto;
    }

    @Override
    public List<ClassDto> toDtoList(List<SchoolClass> classes) {
        if ( classes == null ) {
            return null;
        }

        List<ClassDto> list = new ArrayList<ClassDto>( classes.size() );
        for ( SchoolClass schoolClass : classes ) {
            list.add( toDto( schoolClass ) );
        }

        return list;
    }
}
