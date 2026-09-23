package com.suresh.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RazorpayOrderResponseDTO {
    private String razorpayOrderId;
    private String razorpayKeyId; // sent to frontend to initialize the checkout widget
    private Double amount;
    private String currency;
}