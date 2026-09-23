package com.suresh.ecommerce.service;

import com.suresh.ecommerce.dto.OrderDTO;
import com.suresh.ecommerce.entity.*;
import com.suresh.ecommerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private CacheManager cacheManager;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Cart cart;
    private Product product;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");

        cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);

        product = new Product();
        product.setId(1L);
        product.setName("iPhone 15");
        product.setPrice(BigDecimal.valueOf(79999));
        product.setStock(10);

        cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
    }

    @Test
    void placeOrder_success_deductsStockAndClearsCart() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDTO result = orderService.placeOrder(1L, "Hyderabad");

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(159998), result.getTotalAmount());
        assertEquals(8, product.getStock()); // 10 - 2 = 8
        verify(cartItemRepository).deleteAll(List.of(cartItem)); // cart cleared
        verify(productRepository).save(product); // stock update persisted
    }

    @Test
    void placeOrder_emptyCart_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of()); // empty cart

        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(1L, "Hyderabad"));
    }

    @Test
    void placeOrder_insufficientStock_throwsException() {
        product.setStock(1); // less than the requested quantity (2)

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));

        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(1L, "Hyderabad"));

        // Stock should remain unchanged since the order was rejected before any deduction
        assertEquals(1, product.getStock());
        verify(orderRepository, never()).save(any());
    }
}