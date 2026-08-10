package com.uniwiki.service;

import com.uniwiki.dto.CategoryDto;
import com.uniwiki.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryDto> getCategories() {
        Map<String, NormalizedCategory> categories = new LinkedHashMap<>();
        categoryRepository.findAllByOrderByNameAsc().forEach(category -> {
            boolean repaired = isMojibake(category.getName());
            CategoryDto dto = new CategoryDto(
                    category.getId(),
                    repairUtf8(category.getName()),
                    repairUtf8(category.getDescription())
            );
            categories.merge(dto.name(), new NormalizedCategory(dto, repaired),
                    (current, candidate) -> current.repaired() && !candidate.repaired() ? candidate : current);
        });
        return categories.values().stream()
                .map(NormalizedCategory::dto)
                .toList();
    }

    private boolean isMojibake(String value) {
        return value != null
                && StandardCharsets.ISO_8859_1.newEncoder().canEncode(value)
                && !repairUtf8(value).equals(value);
    }

    private String repairUtf8(String value) {
        if (value == null || !StandardCharsets.ISO_8859_1.newEncoder().canEncode(value)) return value;
        return new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }

    private record NormalizedCategory(CategoryDto dto, boolean repaired) { }
}
