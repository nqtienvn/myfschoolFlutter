package vn.edu.fpt.myfschool.mapper;

import org.mapstruct.*;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.common.dto.ClassDto;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ClassMapper {
    @Mapping(target = "academicYearId", expression = "java(cls.getAcademicYear().getId())")
    @Mapping(target = "academicYearName", expression = "java(cls.getAcademicYear().getName())")
    @Mapping(target = "studentCount", expression = "java(cls.getStudents() != null ? cls.getStudents().size() : 0)")
    ClassDto toDto(SchoolClass cls);

    List<ClassDto> toDtoList(List<SchoolClass> classes);
}
