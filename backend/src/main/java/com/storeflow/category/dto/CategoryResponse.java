package com.storeflow.category.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryResponse {

    private Long id;
    private String categoryName;
    private String description;

}
