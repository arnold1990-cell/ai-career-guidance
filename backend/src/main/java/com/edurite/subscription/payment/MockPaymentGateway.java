package com.edurite.subscription.payment;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentGateway implements PaymentGateway {
    @Override
    public PaymentGatewayResult charge(UUID userId, BigDecimal amount, String currency, String description) {
        return new PaymentGatewayResult("SUCCESS", "MOCK-" + userId + "-" + System.currentTimeMillis(), "mock-gateway");
    }
}
