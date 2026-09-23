package com.suresh.ecommerce.service;

import com.suresh.ecommerce.dto.CategoryDTO;
import com.suresh.ecommerce.entity.Category;
import com.suresh.ecommerce.exception.ResourceNotFoundException;
import com.suresh.ecommerce.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;

    private final ModelMapper modelMapper = new ModelMapper();

    // Constructed manually rather than via @InjectMocks — see UserServiceTest for why.
    private CategoryService categoryService;

    private Category category;
    private CategoryDTO categoryDTO;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, modelMapper);

        category = new Category();
        category.setId(1L);
        category.setName("Electronics");
        category.setDescription("Electronic gadgets and devices");

        categoryDTO = new CategoryDTO();
        categoryDTO.setName("Electronics");
        categoryDTO.setDescription("Electronic gadgets and devices");
    }

    @Test
    void createCategory_success_returnsSavedDto() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CategoryDTO result = categoryService.createCategory(categoryDTO);

        assertNotNull(result);
        assertEquals("Electronics", result.getName());
        assertEquals(1L, result.getId());
    }

    @Test
    void getAllCategories_returnsMappedList() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<CategoryDTO> result = categoryService.getAllCategories();

        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).getName());
    }

    @Test
    void getCategoryById_found_returnsDto() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryDTO result = categoryService.getCategoryById(1L);

        assertEquals("Electronics", result.getName());
    }

    @Test
    void getCategoryById_notFound_throwsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.getCategoryById(99L));
    }

    @Test
    void updateCategory_success_updatesFields() {
        CategoryDTO updateDto = new CategoryDTO();
        updateDto.setName("Consumer Electronics");
        updateDto.setDescription("Updated description");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryDTO result = categoryService.updateCategory(1L, updateDto);

        assertEquals("Consumer Electronics", result.getName());
        assertEquals("Updated description", result.getDescription());
    }

    @Test
    void updateCategory_notFound_throwsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.updateCategory(99L, categoryDTO));

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategory_success_deletesEntity() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_notFound_throwsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.deleteCategory(99L));

        verify(categoryRepository, never()).delete(any());
    }
}
