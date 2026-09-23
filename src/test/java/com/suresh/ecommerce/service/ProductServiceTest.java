package com.suresh.ecommerce.service;

import com.suresh.ecommerce.dto.ProductDTO;
import com.suresh.ecommerce.entity.Category;
import com.suresh.ecommerce.entity.Product;
import com.suresh.ecommerce.exception.ResourceNotFoundException;
import com.suresh.ecommerce.repository.CategoryRepository;
import com.suresh.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;

    // Real instance — a mocked ModelMapper returns null for every mapping call (see UserServiceTest notes)
    private final ModelMapper modelMapper = new ModelMapper();

    // Constructed manually rather than via @InjectMocks — see UserServiceTest for why.
    private ProductService productService;

    private Category category;
    private Product product;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryRepository, modelMapper);

        category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        product = new Product();
        product.setId(1L);
        product.setName("iPhone 15");
        product.setPrice(BigDecimal.valueOf(79999));
        product.setStock(10);
        product.setCategory(category);

        productDTO = new ProductDTO();
        productDTO.setName("iPhone 15");
        productDTO.setPrice(BigDecimal.valueOf(79999));
        productDTO.setStock(10);
        productDTO.setCategoryId(1L);
    }

    @Test
    void createProduct_success_linksCategoryAndSaves() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductDTO result = productService.createProduct(productDTO);

        assertNotNull(result);
        assertEquals("iPhone 15", result.getName());
        assertEquals("Electronics", result.getCategoryName());
        verify(productRepository).save(argThat(p -> p.getCategory().getId().equals(1L)));
    }

    @Test
    void createProduct_categoryNotFound_throwsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        productDTO.setCategoryId(99L);

        assertThrows(ResourceNotFoundException.class,
                () -> productService.createProduct(productDTO));

        verify(productRepository, never()).save(any());
    }

    @Test
    void getProductById_found_returnsDto() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDTO result = productService.getProductById(1L);

        assertEquals("iPhone 15", result.getName());
        assertEquals(1L, result.getCategoryId());
    }

    @Test
    void getProductById_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductById(99L));
    }

    @Test
    void getAllProducts_returnsMappedList() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductDTO> result = productService.getAllProducts();

        assertEquals(1, result.size());
        assertEquals("iPhone 15", result.get(0).getName());
    }

    @Test
    void getProductsByCategory_filtersCorrectly() {
        when(productRepository.findByCategoryId(1L)).thenReturn(List.of(product));

        List<ProductDTO> result = productService.getProductsByCategory(1L);

        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).getCategoryName());
    }

    @Test
    void searchProducts_caseInsensitiveMatch() {
        when(productRepository.findByNameContainingIgnoreCase("iphone")).thenReturn(List.of(product));

        List<ProductDTO> result = productService.searchProducts("iphone");

        assertEquals(1, result.size());
    }

    @Test
    void updateProduct_success_updatesAllFields() {
        ProductDTO updateDto = new ProductDTO();
        updateDto.setName("iPhone 15 Pro");
        updateDto.setPrice(BigDecimal.valueOf(129999));
        updateDto.setStock(5);
        updateDto.setCategoryId(1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDTO result = productService.updateProduct(1L, updateDto);

        assertEquals("iPhone 15 Pro", result.getName());
        assertEquals(BigDecimal.valueOf(129999), result.getPrice());
        assertEquals(5, result.getStock());
    }

    @Test
    void deleteProduct_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.deleteProduct(99L));

        verify(productRepository, never()).delete(any());
    }

    @Test
    void deleteProduct_success_deletesEntity() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
    }
}
