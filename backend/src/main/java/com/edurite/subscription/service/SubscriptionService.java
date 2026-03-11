package com.edurite.subscription.service; // declares the package path for this Java file

import com.edurite.notification.service.NotificationService; // imports a class so it can be used in this file
import com.edurite.security.service.CurrentUserService; // imports a class so it can be used in this file
import com.edurite.subscription.entity.PaymentRecord; // imports a class so it can be used in this file
import com.edurite.subscription.entity.SubscriptionRecord; // imports a class so it can be used in this file
import com.edurite.subscription.repository.PaymentRepository; // imports a class so it can be used in this file
import com.edurite.subscription.repository.SubscriptionRepository; // imports a class so it can be used in this file
import java.math.BigDecimal; // imports a class so it can be used in this file
import java.security.Principal; // imports a class so it can be used in this file
import java.time.LocalDate; // imports a class so it can be used in this file
import java.util.Map; // imports a class so it can be used in this file
import org.springframework.stereotype.Service; // imports a class so it can be used in this file

// @Service marks a class that contains business logic.
@Service // marks this class as a service containing business logic
/**
 * This class named SubscriptionService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class SubscriptionService { // defines a class type
    private final SubscriptionRepository subscriptionRepository; // reads or writes data through the database layer
    private final PaymentRepository paymentRepository; // reads or writes data through the database layer
    private final CurrentUserService currentUserService; // executes this statement as part of the application logic
    private final NotificationService notificationService; // executes this statement as part of the application logic

    public SubscriptionService(SubscriptionRepository subscriptionRepository, PaymentRepository paymentRepository, CurrentUserService currentUserService, NotificationService notificationService) { // reads or writes data through the database layer
        this.subscriptionRepository = subscriptionRepository; // reads or writes data through the database layer
        this.paymentRepository = paymentRepository; // reads or writes data through the database layer
        this.currentUserService = currentUserService; // executes this statement as part of the application logic
        this.notificationService = notificationService; // executes this statement as part of the application logic
    } // ends the current code block

    /**
     * Note: this method handles the "current" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public SubscriptionRecord current(Principal principal) { // declares a method that defines behavior for this class
        var user = currentUserService.requireUser(principal); // executes this statement as part of the application logic
        return subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()).orElseGet(() -> { // returns a value from this method to the caller
            SubscriptionRecord s = new SubscriptionRecord(); // creates a new object instance and stores it in a variable
            s.setUserId(user.getId()); // executes this statement as part of the application logic
            s.setPlanCode("PLAN_BASIC"); // executes this statement as part of the application logic
            s.setStatus("ACTIVE"); // executes this statement as part of the application logic
            s.setRenewalDate(LocalDate.now().plusMonths(1)); // executes this statement as part of the application logic
            s.setStartDate(LocalDate.now()); // executes this statement as part of the application logic
            s.setEndDate(LocalDate.now().plusMonths(1)); // executes this statement as part of the application logic
            return subscriptionRepository.save(s); // returns a value from this method to the caller
        }); // executes this statement as part of the application logic
    } // ends the current code block

    /**
     * Note: this method handles the "purchase" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, Object> purchase(Principal principal, String planCode) { // declares a method that defines behavior for this class
        SubscriptionRecord subscription = current(principal); // executes this statement as part of the application logic
        subscription.setPlanCode("PREMIUM".equalsIgnoreCase(planCode) ? "PLAN_PREMIUM" : "PLAN_BASIC"); // executes this statement as part of the application logic
        subscription.setStatus("ACTIVE"); // executes this statement as part of the application logic
        subscription.setRenewalDate(LocalDate.now().plusMonths(1)); // executes this statement as part of the application logic
        subscription.setStartDate(LocalDate.now()); // executes this statement as part of the application logic
        subscription.setEndDate(LocalDate.now().plusMonths(1)); // executes this statement as part of the application logic
        subscriptionRepository.save(subscription); // reads or writes data through the database layer

        PaymentRecord payment = new PaymentRecord(); // creates a new object instance and stores it in a variable
        payment.setSubscriptionId(subscription.getId()); // executes this statement as part of the application logic
        payment.setAmount("PLAN_PREMIUM".equals(subscription.getPlanCode()) ? new BigDecimal("99.00") : new BigDecimal("0.00")); // executes this statement as part of the application logic
        payment.setCurrency("ZAR"); // executes this statement as part of the application logic
        payment.setStatus("SUCCESS"); // executes this statement as part of the application logic
        paymentRepository.save(payment); // reads or writes data through the database layer
        subscription.setPaymentReference("PAY-" + payment.getId()); // executes this statement as part of the application logic
        subscriptionRepository.save(subscription); // reads or writes data through the database layer

        notificationService.createInApp(subscription.getUserId(), "SUBSCRIPTION", "Subscription updated", // supports the surrounding application logic
                "You are now on " + subscription.getPlanCode().replace("PLAN_", "") + " plan."); // executes this statement as part of the application logic
        return Map.of("subscription", subscription, "payment", payment, "paymentGateway", "placeholder"); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
