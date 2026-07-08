package vn.edu.fpt.myfschool.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.fpt.myfschool.controller.entity.Semester;
import vn.edu.fpt.myfschool.common.dto.SemesterDto;

@Mapper(componentModel = "spring")
public interface SemesterMapper {
    @Mapping(target = "academicYearId", expression = "java(semester.getAcademicYear().getId())")
    @Mapping(target = "academicYearName", expression = "java(semester.getAcademicYear().getName())")
    SemesterDto toDto(Semester semester);
}
