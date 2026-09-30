package com.suresh.ecommerce.controller;

import com.suresh.ecommerce.dto.PaymentDTO;
import com.suresh.ecommerce.dto.PaymentVerificationDTO;
import com.suresh.ecommerce.dto.RazorpayOrderResponseDTO;
import com.suresh.ecommerce.entity.Payment;
import com.suresh.ecommerce.security.CurrentUser;
import com.suresh.ecommerce.service.OrderService;
import com.suresh.ecommerce.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Razorpay payment creation and verification — requires a valid JWT")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final CurrentUser currentUser;

    // Step 1: only the order's owner (or ADMIN) can start payment
    @PostMapping("/create/{orderId}")
    public ResponseEntity<RazorpayOrderResponseDTO> createPayment(Authentication auth,
                                                                  @PathVariable Long orderId) {
        orderService.getOrderByIdForUser(orderId, auth.getName(), currentUser.isAdmin(auth));
        return ResponseEntity.ok(paymentService.createRazorpayOrder(orderId));
    }

    // Step 2: protected by Razorpay signature verification inside the service
    @PostMapping("/verify")
    public ResponseEntity<PaymentDTO> verifyPayment(@Valid @RequestBody PaymentVerificationDTO dto) {
        Payment payment = paymentService.verifyAndCompletePayment(
                dto.getRazorpayOrderId(), dto.getRazorpayPaymentId(), dto.getRazorpaySignature());
        return ResponseEntity.ok(PaymentDTO.from(payment));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentDTO> getPayment(Authentication auth, @PathVariable Long orderId) {
        orderService.getOrderByIdForUser(orderId, auth.getName(), currentUser.isAdmin(auth));
        return ResponseEntity.ok(PaymentDTO.from(paymentService.getPaymentByOrderId(orderId)));
    }
}