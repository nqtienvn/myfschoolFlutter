package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import java.util.List;

public interface FeeCategoryService {
    List<FeeCategoryDto> list();
    FeeCategoryDto create(CreateFeeCategoryRequest request);
    void delete(Long id);
}