package com.storeflow.category.mapper;

import com.storeflow.category.dto.CategoryRequest;
import com.storeflow.category.dto.CategoryResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CategoryMapper {

    List<CategoryResponse> findAll();

    boolean existsById(Long id);

    boolean existsByCategoryName(@Param("categoryName") String categoryName, @Param("excludeId") Long excludeId);

    Long insert(CategoryRequest request);

    int update(@Param("id") Long id, @Param("request") CategoryRequest request);

}
