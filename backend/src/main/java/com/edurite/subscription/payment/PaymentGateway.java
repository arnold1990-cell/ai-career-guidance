package com.edurite.subscription.payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {
    PaymentGatewayResult charge(UUID userId, BigDecimal amount, String currency, String description);
}
