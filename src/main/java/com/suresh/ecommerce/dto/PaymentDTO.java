package com.suresh.ecommerce.dto;

import com.suresh.ecommerce.entity.Payment;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentDTO {
    private Long id;
    private Long orderId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paymentDate;

    public static PaymentDTO from(Payment p) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(p.getId());
        dto.setOrderId(p.getOrder().getId());
        dto.setRazorpayOrderId(p.getRazorpayOrderId());
        dto.setRazorpayPaymentId(p.getRazorpayPaymentId());
        dto.setAmount(p.getAmount());
        dto.setStatus(p.getStatus().name());
        dto.setPaymentDate(p.getPaymentDate());
        return dto;
    }
}