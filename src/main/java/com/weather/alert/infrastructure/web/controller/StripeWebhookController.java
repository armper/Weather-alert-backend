package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.MessageResponse;
import com.weather.alert.application.usecase.HandleStripeWebhookUseCase;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
@Hidden
public class StripeWebhookController {

    private final HandleStripeWebhookUseCase handleStripeWebhookUseCase;

    @PostMapping("/webhook")
    public ResponseEntity<MessageResponse> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader) {
        handleStripeWebhookUseCase.handle(payload, signatureHeader);
        return ResponseEntity.ok(MessageResponse.builder().message("Webhook processed").build());
    }
}
