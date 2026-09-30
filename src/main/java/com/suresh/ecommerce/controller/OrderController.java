package com.suresh.ecommerce.controller;

import com.suresh.ecommerce.dto.OrderDTO;
import com.suresh.ecommerce.entity.Order;
import com.suresh.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement and tracking — requires a valid JWT")
public class OrderController {

    private final OrderService orderService;

    // Place an order for the logged-in user (userId comes from the JWT, not the URL)
    @PostMapping("/place")
    public ResponseEntity<OrderDTO> placeOrder(Authentication auth,
                                               @RequestParam String shippingAddress) {
        Long userId = orderService.getUserIdByEmail(auth.getName());
        return new ResponseEntity<>(orderService.placeOrder(userId, shippingAddress), HttpStatus.CREATED);
    }

    // Logged-in user's own orders
    @GetMapping("/my")
    public ResponseEntity<List<OrderDTO>> getMyOrders(Authentication auth) {
        Long userId = orderService.getUserIdByEmail(auth.getName());
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    // ADMIN only (enforced in SecurityConfig)
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDTO>> getOrdersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    // Owner or ADMIN
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(Authentication auth, @PathVariable Long orderId) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(orderService.getOrderByIdForUser(orderId, auth.getName(), isAdmin));
    }

    // ADMIN only (enforced in SecurityConfig)
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderDTO> updateStatus(@PathVariable Long orderId,
                                                 @RequestParam Order.Status status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }
}