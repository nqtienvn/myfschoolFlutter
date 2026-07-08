package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.CreateSchoolRequest;
import vn.edu.fpt.myfschool.common.dto.SchoolDto;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.School;
import vn.edu.fpt.myfschool.repository.SchoolRepository;
import vn.edu.fpt.myfschool.service.SchoolService;

import java.util.List;

@Service("schoolService")
@RequiredArgsConstructor
@Transactional
public class SchoolServiceImpl implements SchoolService {

    private final SchoolRepository schoolRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SchoolDto> listSchools() {
        return schoolRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public SchoolDto createSchool(CreateSchoolRequest request) {
        if (schoolRepository.existsByName(request.name().trim())) {
            throw new ConflictException("Tên cơ sở đã tồn tại");
        }
        if (schoolRepository.existsByCode(request.code().trim())) {
            throw new ConflictException("Mã cơ sở đã tồn tại");
        }

        School school = new School();
        school.setName(request.name().trim());
        school.setCode(request.code().trim());
        school.setAddress(trim(request.address()));
        school.setPhone(trim(request.phone()));
        school.setSchoolName(request.schoolName().trim());
        return toDto(schoolRepository.save(school));
    }

    @Override
    public SchoolDto updateSchool(Long id, CreateSchoolRequest request) {
        School school = schoolRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("School", "id", id));

        schoolRepository.findByCode(request.code().trim())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> { throw new ConflictException("Mã cơ sở đã tồn tại ở cơ sở khác"); });

        school.setName(request.name().trim());
        school.setCode(request.code().trim());
        school.setAddress(trim(request.address()));
        school.setPhone(trim(request.phone()));
        school.setSchoolName(request.schoolName().trim());
        return toDto(schoolRepository.save(school));
    }

    @Override
    public void deleteSchool(Long id) {
        School school = schoolRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("School", "id", id));
        schoolRepository.delete(school);
    }

    private SchoolDto toDto(School school) {
        return new SchoolDto(school.getId(), school.getName(), school.getCode(), school.getAddress(), school.getPhone(), school.getSchoolName());
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
