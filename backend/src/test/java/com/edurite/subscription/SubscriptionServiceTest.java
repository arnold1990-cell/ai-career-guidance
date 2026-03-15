package com.edurite.subscription;

import com.edurite.notification.service.NotificationService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.payment.PaymentGateway;
import com.edurite.subscription.payment.PaymentGatewayResult;
import com.edurite.subscription.repository.PaymentRepository;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.subscription.service.SubscriptionService;
import com.edurite.user.entity.User;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubscriptionServiceTest {
    @Test
    void purchaseUsesPaymentGatewayAndUpdatesSubscription() {
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        NotificationService notificationService = mock(NotificationService.class);
        PaymentGateway paymentGateway = mock(PaymentGateway.class);

        SubscriptionService service = new SubscriptionService(subscriptionRepository, paymentRepository, currentUserService, notificationService, paymentGateway);

        User user = new User(); user.setId(UUID.randomUUID());
        when(currentUserService.requireUser(any())).thenReturn(user);

        SubscriptionRecord existing = new SubscriptionRecord();
        existing.setId(UUID.randomUUID());
        existing.setUserId(user.getId());
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(paymentGateway.charge(any(), any(), any(), any())).thenReturn(new PaymentGatewayResult("SUCCESS", "MOCK-1", "mock-gateway"));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = service.purchase((Principal) () -> "student@test", "PREMIUM");

        verify(paymentGateway).charge(eq(user.getId()), any(), eq("ZAR"), any());
        assertThat(((SubscriptionRecord) response.get("subscription")).getPlanCode()).isEqualTo("PLAN_PREMIUM");
        assertThat(response.get("paymentGateway")).isEqualTo("mock-gateway");
    }
}
