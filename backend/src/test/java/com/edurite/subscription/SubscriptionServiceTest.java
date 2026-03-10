package com.edurite.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edurite.common.exception.InvalidCredentialsException;
import com.edurite.notification.service.NotificationService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.repository.PaymentRepository;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.subscription.service.SubscriptionService;
import com.edurite.user.entity.User;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private NotificationService notificationService;

    private SubscriptionService subscriptionService;
    private Principal principal;
    private User user;

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(subscriptionRepository, paymentRepository, currentUserService, notificationService);
        principal = () -> "student@example.com";
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("student@example.com");
    }

    @Test
    void currentReturnsExistingSubscriptionForAuthenticatedStudent() {
        SubscriptionRecord existing = new SubscriptionRecord();
        existing.setUserId(user.getId());
        existing.setPlanCode("PLAN_PREMIUM");
        existing.setStatus("ACTIVE");

        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(existing));

        SubscriptionRecord result = subscriptionService.current(principal);

        assertThat(result.getPlanCode()).isEqualTo("PLAN_PREMIUM");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void currentReturnsFallbackWhenRepositoryFails() {
        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())).thenThrow(new RuntimeException("db down"));

        SubscriptionRecord result = subscriptionService.current(principal);

        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getPlanCode()).isEqualTo("PLAN_BASIC");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void purchaseCreatesPaymentAndNotification() {
        SubscriptionRecord existing = new SubscriptionRecord();
        existing.setId(UUID.randomUUID());
        existing.setUserId(user.getId());
        existing.setPlanCode("PLAN_BASIC");
        existing.setStatus("ACTIVE");

        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any(SubscriptionRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = subscriptionService.purchase(principal, "PREMIUM");

        SubscriptionRecord subscription = (SubscriptionRecord) result.get("subscription");
        assertThat(subscription.getPlanCode()).isEqualTo("PLAN_PREMIUM");
        verify(paymentRepository).save(any());
        verify(notificationService).createInApp(user.getId(), "SUBSCRIPTION", "Subscription updated", "You are now on PREMIUM plan.");
    }

    @Test
    void currentThrowsForUnauthenticatedPrincipal() {
        when(currentUserService.requireUser(principal)).thenThrow(new InvalidCredentialsException());

        assertThatThrownBy(() -> subscriptionService.current(principal)).isInstanceOf(InvalidCredentialsException.class);
    }
}
