package com.uniwiki.service;

import com.uniwiki.entity.Category;
import com.uniwiki.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void returnsCategoriesInRepositoryOrder() {
        Category academic = category(1L, "학사");
        Category campus = category(2L, "학교생활");
        when(categoryRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(academic, campus));

        assertThat(categoryService.getCategories())
                .extracting("name")
                .containsExactly("학사", "학교생활");
    }

    @Test
    void repairsMojibakeAndPrefersTheCleanDuplicate() {
        Category broken = category(1L, mojibake("학사"));
        Category clean = category(171L, "학사");
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(broken, clean));

        assertThat(categoryService.getCategories())
                .singleElement()
                .satisfies(category -> {
                    assertThat(category.id()).isEqualTo(171L);
                    assertThat(category.name()).isEqualTo("학사");
                });
    }

    private String mojibake(String value) {
        return new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }

    private Category category(Long id, String name) {
        Category category = mock(Category.class);
        when(category.getId()).thenReturn(id);
        when(category.getName()).thenReturn(name);
        return category;
    }
}
