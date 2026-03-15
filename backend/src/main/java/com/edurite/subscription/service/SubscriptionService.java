package com.edurite.subscription.service;

import com.edurite.notification.service.NotificationService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.subscription.entity.PaymentRecord;
import com.edurite.subscription.payment.PaymentGateway;
import com.edurite.subscription.payment.PaymentGatewayResult;
import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.repository.PaymentRepository;
import com.edurite.subscription.repository.SubscriptionRepository;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;

// @Service marks a class that contains business logic.
@Service
/**
 * This class named SubscriptionService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final PaymentGateway paymentGateway;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, PaymentRepository paymentRepository, CurrentUserService currentUserService, NotificationService notificationService, PaymentGateway paymentGateway) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.paymentGateway = paymentGateway;
    }

    /**
     * this method handles the "current" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public SubscriptionRecord current(Principal principal) {
        var user = currentUserService.requireUser(principal);
        return subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()).orElseGet(() -> {
            SubscriptionRecord s = new SubscriptionRecord();
            s.setUserId(user.getId());
            s.setPlanCode("PLAN_BASIC");
            s.setStatus("ACTIVE");
            s.setRenewalDate(LocalDate.now().plusMonths(1));
            s.setStartDate(LocalDate.now());
            s.setEndDate(LocalDate.now().plusMonths(1));
            return subscriptionRepository.save(s);
        });
    }

    /**
     * this method handles the "purchase" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, Object> purchase(Principal principal, String planCode) {
        SubscriptionRecord subscription = current(principal);
        subscription.setPlanCode("PREMIUM".equalsIgnoreCase(planCode) ? "PLAN_PREMIUM" : "PLAN_BASIC");
        subscription.setStatus("ACTIVE");
        subscription.setRenewalDate(LocalDate.now().plusMonths(1));
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusMonths(1));
        subscriptionRepository.save(subscription);

        BigDecimal amount = "PLAN_PREMIUM".equals(subscription.getPlanCode()) ? new BigDecimal("99.00") : new BigDecimal("0.00");
        PaymentGatewayResult gatewayResult = paymentGateway.charge(subscription.getUserId(), amount, "ZAR", "EduRite subscription purchase");

        PaymentRecord payment = new PaymentRecord();
        payment.setSubscriptionId(subscription.getId());
        payment.setAmount(amount);
        payment.setCurrency("ZAR");
        payment.setStatus(gatewayResult.status());
        paymentRepository.save(payment);
        subscription.setPaymentReference(gatewayResult.reference());
        subscriptionRepository.save(subscription);

        notificationService.createInApp(subscription.getUserId(), "SUBSCRIPTION", "Subscription updated",
                "You are now on " + subscription.getPlanCode().replace("PLAN_", "") + " plan.");
        return Map.of("subscription", subscription, "payment", payment, "paymentGateway", gatewayResult.provider());
    }
}
