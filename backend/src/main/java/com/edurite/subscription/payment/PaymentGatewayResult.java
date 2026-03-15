package com.edurite.subscription.payment;

public record PaymentGatewayResult(String status, String reference, String provider) {}
