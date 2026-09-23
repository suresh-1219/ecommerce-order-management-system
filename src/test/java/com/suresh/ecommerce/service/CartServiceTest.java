package com.suresh.ecommerce.service;

import com.suresh.ecommerce.dto.CartDTO;
import com.suresh.ecommerce.entity.*;
import com.suresh.ecommerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Cart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);

        product = new Product();
        product.setId(1L);
        product.setName("iPhone 15");
        product.setPrice(BigDecimal.valueOf(1000));
        product.setStock(10);
    }

    @Test
    void addItemToCart_newProduct_createsCartItem() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());

        CartDTO result = cartService.addItemToCart(1L, 1L, 2);

        verify(cartItemRepository).save(any(CartItem.class));
        assertNotNull(result);
    }

    @Test
    void addItemToCart_duplicateProduct_increasesQuantityInsteadOfDuplicating() {
        CartItem existingItem = new CartItem();
        existingItem.setId(1L);
        existingItem.setCart(cart);
        existingItem.setProduct(product);
        existingItem.setQuantity(1);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(existingItem));

        cartService.addItemToCart(1L, 1L, 2);

        assertEquals(3, existingItem.getQuantity()); // 1 (existing) + 2 (new) = 3
        verify(cartItemRepository, never()).save(argThat(item -> item != existingItem));
    }

    @Test
    void addItemToCart_insufficientStock_throwsException() {
        product.setStock(1); // less than requested quantity (5)

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class,
                () -> cartService.addItemToCart(1L, 1L, 5));
    }

    @Test
    void getCartByUserId_calculatesTotalCorrectly() {
        CartItem item1 = new CartItem();
        item1.setProduct(product); // price 1000
        item1.setQuantity(2); // subtotal 2000

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Case");
        product2.setPrice(BigDecimal.valueOf(500));

        CartItem item2 = new CartItem();
        item2.setProduct(product2);
        item2.setQuantity(1); // subtotal 500

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(item1, item2));

        CartDTO result = cartService.getCartByUserId(1L);

        assertEquals(BigDecimal.valueOf(2500), result.getTotalAmount()); // 2000 + 500
    }
}