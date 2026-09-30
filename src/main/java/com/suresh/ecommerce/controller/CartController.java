package com.suresh.ecommerce.controller;

import com.suresh.ecommerce.dto.CartDTO;
import com.suresh.ecommerce.security.CurrentUser;
import com.suresh.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart — requires a valid JWT")
public class CartController {

    private final CartService cartService;
    private final CurrentUser currentUser;

    @GetMapping("/my")
    public ResponseEntity<CartDTO> getCart(Authentication auth) {
        return ResponseEntity.ok(cartService.getCartByUserId(currentUser.id(auth)));
    }

    @PostMapping("/add")
    public ResponseEntity<CartDTO> addItem(
            Authentication auth,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.addItemToCart(currentUser.id(auth), productId, quantity));
    }

    @PutMapping("/item/{cartItemId}")
    public ResponseEntity<Void> updateQuantity(
            Authentication auth,
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity) {
        cartService.updateItemQuantity(currentUser.id(auth), cartItemId, quantity);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/item/{cartItemId}")
    public ResponseEntity<Void> removeItem(Authentication auth, @PathVariable Long cartItemId) {
        cartService.removeItemFromCart(currentUser.id(auth), cartItemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(Authentication auth) {
        cartService.clearCart(currentUser.id(auth));
        return ResponseEntity.noContent().build();
    }
}