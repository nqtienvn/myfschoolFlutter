package vn.edu.fpt.myfschool.mapper;

import org.mapstruct.Mapper;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.common.dto.SemesterDto;

@Mapper(componentModel = "spring")
public interface SemesterMapper {
    SemesterDto toDto(Semester semester);
}
