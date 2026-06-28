package vn.edu.fpt.myfschool.mapper;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import vn.edu.fpt.myfschool.common.dto.SubjectDto;
import vn.edu.fpt.myfschool.entity.Subject;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-28T20:44:02+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class SubjectMapperImpl implements SubjectMapper {

    @Override
    public SubjectDto toDto(Subject subject) {
        if ( subject == null ) {
            return null;
        }

        Long id = null;
        String name = null;
        String code = null;

        id = subject.getId();
        name = subject.getName();
        code = subject.getCode();

        SubjectDto subjectDto = new SubjectDto( id, name, code );

        return subjectDto;
    }
}
