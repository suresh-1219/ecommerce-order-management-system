package com.suresh.ecommerce.controller;

import com.suresh.ecommerce.dto.ProductDTO;
import com.suresh.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog — browsing is public, management requires ADMIN")public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductDTO dto) {
        ProductDTO created = productService.createProduct(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @SecurityRequirements
    @GetMapping("/paged")
    @Operation(summary = "List products with pagination and sorting",
            description = "Example: /api/products/paged?page=0&size=10&sort=price,asc")
    public ResponseEntity<Page<ProductDTO>> getAllProductsPaged(Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProductsPaged(pageable));
    }

    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @SecurityRequirements
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    @SecurityRequirements
    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> searchProducts(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDTO dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}