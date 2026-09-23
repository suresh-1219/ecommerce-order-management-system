package com.suresh.ecommerce.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOrderConfirmation(String toEmail, Long orderId, String customerName, String totalAmount) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Order Confirmation - Order #" + orderId);
        message.setText(
                "Hi " + customerName + ",\n\n" +
                "Thank you for your order! Your order has been placed successfully.\n\n" +
                "Order ID: #" + orderId + "\n" +
                "Total Amount: Rs. " + totalAmount + "\n\n" +
                "We'll notify you once your order is shipped.\n\n" +
                "Thanks for shopping with us!\n" +
                "E-Commerce Team"
        );
        mailSender.send(message);
    }
}