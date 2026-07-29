package com.uniwiki.dto;

import com.uniwiki.entity.Category;

public record CategoryDto(Long id, String name, String description) {
    public static CategoryDto from(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}
