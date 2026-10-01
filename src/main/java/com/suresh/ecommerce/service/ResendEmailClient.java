package com.suresh.ecommerce.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class ResendEmailClient {

    @Value("${resend.api.key:}")
    private String apiKey;

    @Value("${resend.from:Order Demo <onboarding@resend.dev>}")
    private String from;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public void send(String to, String subject, String html) throws Exception {
        JSONObject body = new JSONObject()
                .put("from", from)
                .put("to", new JSONArray().put(to))
                .put("subject", subject)
                .put("html", html);

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new RuntimeException("Resend API error " + response.statusCode() + ": " + response.body());
        }
    }
}