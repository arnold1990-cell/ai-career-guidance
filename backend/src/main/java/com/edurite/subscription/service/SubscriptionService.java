package com.edurite.subscription.service;

import com.edurite.notification.service.NotificationService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.subscription.entity.PaymentRecord;
import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.repository.PaymentRepository;
import com.edurite.subscription.repository.SubscriptionRepository;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionService {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, PaymentRepository paymentRepository, CurrentUserService currentUserService, NotificationService notificationService) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    public SubscriptionRecord current(Principal principal) {
        var user = currentUserService.requireUser(principal);
        try {
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
        } catch (RuntimeException ex) {
            log.error("Failed to load subscription for user {}", user.getId(), ex);
            SubscriptionRecord fallback = new SubscriptionRecord();
            fallback.setUserId(user.getId());
            fallback.setPlanCode("PLAN_BASIC");
            fallback.setStatus("ACTIVE");
            fallback.setRenewalDate(LocalDate.now().plusMonths(1));
            fallback.setStartDate(LocalDate.now());
            fallback.setEndDate(LocalDate.now().plusMonths(1));
            return fallback;
        }
    }

    public Map<String, Object> purchase(Principal principal, String planCode) {
        SubscriptionRecord subscription = current(principal);
        subscription.setPlanCode("PREMIUM".equalsIgnoreCase(planCode) ? "PLAN_PREMIUM" : "PLAN_BASIC");
        subscription.setStatus("ACTIVE");
        subscription.setRenewalDate(LocalDate.now().plusMonths(1));
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusMonths(1));
        subscriptionRepository.save(subscription);

        PaymentRecord payment = new PaymentRecord();
        payment.setSubscriptionId(subscription.getId());
        payment.setAmount("PLAN_PREMIUM".equals(subscription.getPlanCode()) ? new BigDecimal("99.00") : new BigDecimal("0.00"));
        payment.setCurrency("ZAR");
        payment.setStatus("SUCCESS");
        paymentRepository.save(payment);
        subscription.setPaymentReference("PAY-" + payment.getId());
        subscriptionRepository.save(subscription);

        notificationService.createInApp(subscription.getUserId(), "SUBSCRIPTION", "Subscription updated",
                "You are now on " + subscription.getPlanCode().replace("PLAN_", "") + " plan.");
        return Map.of("subscription", subscription, "payment", payment, "paymentGateway", "placeholder");
    }
}
