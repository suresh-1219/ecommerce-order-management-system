package com.suresh.ecommerce.controller;

import com.suresh.ecommerce.dto.PaymentVerificationDTO;
import com.suresh.ecommerce.dto.RazorpayOrderResponseDTO;
import com.suresh.ecommerce.entity.Payment;
import com.suresh.ecommerce.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Razorpay payment creation and verification — requires a valid JWT")
public class PaymentController {

    private final PaymentService paymentService;

    // Step 1: Called right after an order is placed, to start the payment process
    @PostMapping("/create/{orderId}")
    public ResponseEntity<RazorpayOrderResponseDTO> createPayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.createRazorpayOrder(orderId));
    }

    // Step 2: Called after the client completes checkout in the Razorpay widget
    @PostMapping("/verify")
    public ResponseEntity<Payment> verifyPayment(@Valid @RequestBody PaymentVerificationDTO dto) {
        Payment payment = paymentService.verifyAndCompletePayment(
                dto.getRazorpayOrderId(),
                dto.getRazorpayPaymentId(),
                dto.getRazorpaySignature()
        );
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }
}