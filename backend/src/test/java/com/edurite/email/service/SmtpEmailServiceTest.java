package com.edurite.email.service;

import com.edurite.auth.config.MailSenderProperties;
import com.edurite.auth.exception.EmailDispatchException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpEmailServiceTest {

    @Test
    void sendsVerificationEmailUsingMimeMessage() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        SmtpEmailService service = new SmtpEmailService(mailSender, new MailSenderProperties("no-reply@edurite.local"));

        service.sendEmailVerification("student@example.com", "Student", "http://localhost:5173/verify-email?token=abc", 24);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void wrapsMailFailuresInEmailDispatchException() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP unavailable")).when(mailSender).send(mimeMessage);
        SmtpEmailService service = new SmtpEmailService(mailSender, new MailSenderProperties("no-reply@edurite.local"));

        assertThrows(
                EmailDispatchException.class,
                () -> service.sendEmailVerification(
                        "student@example.com",
                        "Student",
                        "http://localhost:5173/verify-email?token=abc",
                        24
                )
        );
    }
}
