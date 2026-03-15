package com.edurite.notification.service;

import com.edurite.notification.events.BursaryDeadlineReminderEvent;
import com.edurite.notification.events.CareerInsightUpdateEvent;
import com.edurite.notification.events.NewBursaryPublishedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

    private final ApplicationEventPublisher applicationEventPublisher;

    public NotificationScheduler(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Scheduled(cron = "${edurite.notifications.deadline-reminders.cron:0 0 8 * * *}")
    public void publishDeadlineReminderJob() {
        applicationEventPublisher.publishEvent(new BursaryDeadlineReminderEvent(null, "Upcoming bursary deadlines"));
    }

    @Scheduled(cron = "${edurite.notifications.new-bursary.cron:0 0 9 * * *}")
    public void publishNewBursaryAlertsJob() {
        applicationEventPublisher.publishEvent(new NewBursaryPublishedEvent(null, "New bursary opportunities"));
    }

    @Scheduled(cron = "${edurite.notifications.career-insights.cron:0 0 10 * * MON}")
    public void publishCareerInsightJob() {
        applicationEventPublisher.publishEvent(new CareerInsightUpdateEvent("Weekly Career Insight", "New labour market and skill trend guidance is available."));
    }
}
