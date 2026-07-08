package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.FeeCategory;
import vn.edu.fpt.myfschool.repository.FeeCategoryRepository;
import vn.edu.fpt.myfschool.service.FeeCategoryService;

import java.util.List;

@Service("feeCategoryService")
@RequiredArgsConstructor
@Transactional
public class FeeCategoryServiceImpl implements FeeCategoryService {

    private final FeeCategoryRepository feeCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FeeCategoryDto> list() {
        return feeCategoryRepository.findAll().stream()
            .map(c -> new FeeCategoryDto(c.getId(), c.getName(), c.getDescription()))
            .toList();
    }

    @Override
    public FeeCategoryDto create(CreateFeeCategoryRequest request) {
        if (feeCategoryRepository.existsByName(request.name())) {
            throw new ConflictException("Danh muc phi da ton tai");
        }
        FeeCategory fc = new FeeCategory();
        fc.setName(request.name());
        fc.setDescription(request.description());
        fc = feeCategoryRepository.save(fc);
        return new FeeCategoryDto(fc.getId(), fc.getName(), fc.getDescription());
    }

    @Override
    public void delete(Long id) {
        FeeCategory fc = feeCategoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FeeCategory", "id", id));
        feeCategoryRepository.delete(fc);
    }
}