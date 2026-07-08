package vn.edu.fpt.myfschool.mapper;

import org.mapstruct.Mapper;
import vn.edu.fpt.myfschool.controller.entity.Subject;
import vn.edu.fpt.myfschool.common.dto.SubjectDto;

@Mapper(componentModel = "spring")
public interface SubjectMapper {
    SubjectDto toDto(Subject subject);
}
