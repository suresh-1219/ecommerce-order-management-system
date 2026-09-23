package com.suresh.ecommerce.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.suresh.ecommerce.dto.RazorpayOrderResponseDTO;
import com.suresh.ecommerce.entity.Order;
import com.suresh.ecommerce.entity.Payment;
import com.suresh.ecommerce.exception.ResourceNotFoundException;
import com.suresh.ecommerce.repository.OrderRepository;
import com.suresh.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // Step 1: Create a Razorpay order for an already-placed local order
    public RazorpayOrderResponseDTO createRazorpayOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        try {
            JSONObject orderRequest = new JSONObject();
            // Razorpay expects amount in the smallest currency unit (paise for INR)
            int amountInPaise = order.getTotalAmount().multiply(java.math.BigDecimal.valueOf(100)).intValue();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_rcpt_" + order.getId());

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            // Save a PENDING payment record locally, linked to this Razorpay order
            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setRazorpayOrderId(razorpayOrderId);
            payment.setAmount(order.getTotalAmount());
            payment.setStatus(Payment.Status.PENDING);
            paymentRepository.save(payment);

            return new RazorpayOrderResponseDTO(razorpayOrderId, razorpayKeyId,
                    order.getTotalAmount().doubleValue(), "INR");

        } catch (RazorpayException e) {
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage());
        }
    }

    // Step 2: Verify the payment signature after checkout completes on the client side
    public Payment verifyAndCompletePayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + razorpayOrderId));

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);

            boolean isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);

            if (isValid) {
                payment.setRazorpayPaymentId(razorpayPaymentId);
                payment.setStatus(Payment.Status.SUCCESS);
                payment.setPaymentDate(LocalDateTime.now());

                // Also update the order status
                Order order = payment.getOrder();
                order.setStatus(Order.Status.CONFIRMED);
                orderRepository.save(order);
            } else {
                payment.setStatus(Payment.Status.FAILED);
            }

            return paymentRepository.save(payment);

        } catch (RazorpayException e) {
            payment.setStatus(Payment.Status.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Signature verification failed: " + e.getMessage());
        }
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));
    }
}