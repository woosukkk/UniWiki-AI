package com.uniwiki.service;

import com.uniwiki.dto.CategoryDto;
import com.uniwiki.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
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
        return value != null && !repairUtf8(value).equals(value);
    }

    private String repairUtf8(String value) {
        if (value == null) return null;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(value.length());
        for (char character : value.toCharArray()) {
            if (character <= 0xff) {
                bytes.write(character);
                continue;
            }
            String text = String.valueOf(character);
            if (!java.nio.charset.Charset.forName("windows-1252").newEncoder().canEncode(text)) return value;
            bytes.writeBytes(text.getBytes(java.nio.charset.Charset.forName("windows-1252")));
        }
        String decoded = bytes.toString(StandardCharsets.UTF_8);
        return decoded.matches(".*[가-힣].*") ? decoded : value;
    }

    private record NormalizedCategory(CategoryDto dto, boolean repaired) { }
}
