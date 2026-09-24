package com.storeflow.category.service;

import com.storeflow.category.dto.CategoryRequest;
import com.storeflow.category.dto.CategoryResponse;
import com.storeflow.category.mapper.CategoryMapper;
import com.storeflow.common.exception.BusinessException;
import com.storeflow.common.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public List<CategoryResponse> findAll() {
        return categoryMapper.findAll();
    }

    public void requireExists(Long id) {
        if (!categoryMapper.existsById(id)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    @Transactional
    public Long create(CategoryRequest request) {
        if (categoryMapper.existsByCategoryName(request.categoryName(), null)) {
            throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY_NAME);
        }
        return categoryMapper.insert(request);
    }

    @Transactional
    public void update(Long id, CategoryRequest request) {
        requireExists(id);
        if (categoryMapper.existsByCategoryName(request.categoryName(), id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY_NAME);
        }
        categoryMapper.update(id, request);
    }

}
