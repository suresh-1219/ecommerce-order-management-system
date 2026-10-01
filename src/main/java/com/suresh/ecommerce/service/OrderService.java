package com.suresh.ecommerce.service;

import com.suresh.ecommerce.dto.OrderDTO;
import com.suresh.ecommerce.dto.OrderItemDTO;
import com.suresh.ecommerce.entity.*;
import com.suresh.ecommerce.exception.ResourceNotFoundException;
import com.suresh.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final CacheManager cacheManager;
    
    @Transactional
    public OrderDTO placeOrder(Long userId, String shippingAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cannot place order with an empty cart");
        }

        // Step 1: Validate stock for every item BEFORE making any changes
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            if (product.getStock() < item.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }
        }

        // Step 2: Create the Order
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(Order.Status.PLACED);
        order.setShippingAddress(shippingAddress);

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Step 3: Convert each CartItem into an OrderItem + deduct stock
        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
            Product product = cartItem.getProduct();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice()); // snapshot the current price

            // Deduct stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            // Keep the Redis-cached product in sync — without this, GET /api/products/{id}
            // would keep serving the pre-order stock count until the cache's 10-minute TTL expires.
            evictProductCache(product.getId());

            return orderItem;
        }).collect(Collectors.toList());

        for (OrderItem oi : orderItems) {
            totalAmount = totalAmount.add(oi.getPriceAtPurchase().multiply(BigDecimal.valueOf(oi.getQuantity())));
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);

        // Step 4: Clear the cart after successful order placement
        cartItemRepository.deleteAll(cartItems);

        // Email is sent asynchronously; failures are logged inside EmailService
        // and never affect order placement.
        emailService.sendOrderConfirmation(
                user.getEmail(),
                savedOrder.getId(),
                user.getName(),
                totalAmount.toString()
        );

        return mapToDTO(savedOrder, orderItems);
     
    }

    public List<OrderDTO> getOrdersByUser(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(order -> mapToDTO(order, orderItemRepository.findByOrderId(order.getId())))
                .collect(Collectors.toList());
    }

    public Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }

    public OrderDTO getOrderByIdForUser(Long orderId, String email, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        if (!isAdmin && !order.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("You can only view your own orders");
        }
        return mapToDTO(order, orderItemRepository.findByOrderId(orderId));
    }
    
    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return mapToDTO(order, items);
    }

    public OrderDTO updateOrderStatus(Long orderId, Order.Status newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return mapToDTO(updated, items);
    }

    private OrderDTO mapToDTO(Order order, List<OrderItem> items) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus().name());
        dto.setShippingAddress(order.getShippingAddress());

        List<OrderItemDTO> itemDTOs = items.stream().map(item -> {
            OrderItemDTO idto = new OrderItemDTO();
            idto.setId(item.getId());
            idto.setProductId(item.getProduct().getId());
            idto.setProductName(item.getProduct().getName());
            idto.setQuantity(item.getQuantity());
            idto.setPriceAtPurchase(item.getPriceAtPurchase());
            return idto;
        }).collect(Collectors.toList());

        dto.setItems(itemDTOs);
        return dto;
    }

    // Evicts a single product from both product caches. Done programmatically (via
    // CacheManager) rather than @CacheEvict, since the product id here comes from a
    // cart item processed inside a stream, not from placeOrder()'s own method params.
    private void evictProductCache(Long productId) {
        Cache byId = cacheManager.getCache("productById");
        if (byId != null) {
            byId.evict(productId);
        }
        Cache list = cacheManager.getCache("productsList");
        if (list != null) {
            list.clear();
        }
    }
}