package com.suresh.ecommerce.service;

import com.suresh.ecommerce.dto.CartDTO;
import com.suresh.ecommerce.dto.CartItemDTO;
import com.suresh.ecommerce.entity.Cart;
import com.suresh.ecommerce.entity.CartItem;
import com.suresh.ecommerce.entity.Product;
import com.suresh.ecommerce.entity.User;
import com.suresh.ecommerce.exception.ResourceNotFoundException;
import com.suresh.ecommerce.repository.CartItemRepository;
import com.suresh.ecommerce.repository.CartRepository;
import com.suresh.ecommerce.repository.ProductRepository;
import com.suresh.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // Get user's cart, or create a new empty one if it doesn't exist yet
    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    public CartDTO addItemToCart(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        Cart cart = getOrCreateCart(userId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElse(null);

        int alreadyInCart = existingItem != null ? existingItem.getQuantity() : 0;
        if (product.getStock() < alreadyInCart + quantity) {
            throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
        }

        if (existingItem != null) {
            existingItem.setQuantity(alreadyInCart + quantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            cartItemRepository.save(newItem);
        }

        return getCartByUserId(userId);
    }

    public CartDTO getCartByUserId(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        List<CartItemDTO> itemDTOs = items.stream()
                .map(this::mapToItemDTO)
                .collect(Collectors.toList());

        BigDecimal total = itemDTOs.stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());
        dto.setUserId(userId);
        dto.setItems(itemDTOs);
        dto.setTotalAmount(total);
        return dto;
    }

    @Transactional
    public void updateItemQuantity(Long userId, Long cartItemId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        CartItem item = getOwnedItem(userId, cartItemId);

        if (item.getProduct().getStock() < quantity) {
            throw new IllegalArgumentException("Insufficient stock for product: " + item.getProduct().getName());
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    @Transactional
    public void removeItemFromCart(Long userId, Long cartItemId) {
        cartItemRepository.delete(getOwnedItem(userId, cartItemId));
    }

    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        cartItemRepository.deleteAll(items);
    }

    private CartItemDTO mapToItemDTO(CartItem item) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setProductPrice(item.getProduct().getPrice());
        dto.setQuantity(item.getQuantity());
        dto.setSubtotal(item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return dto;
    }
    
    private CartItem getOwnedItem(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));
        if (!item.getCart().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only modify your own cart items");
        }
        return item;
    }
}