package com.edurite.email.service;

import com.edurite.auth.config.MailSenderProperties;
import com.edurite.auth.exception.EmailDispatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final MailSenderProperties mailSenderProperties;

    public SmtpEmailService(JavaMailSender mailSender, MailSenderProperties mailSenderProperties) {
        this.mailSender = mailSender;
        this.mailSenderProperties = mailSenderProperties;
    }

    @Override
    public void sendEmailVerification(String toEmail, String recipientName, String verificationUrl, long expiresInHours) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailSenderProperties.senderEmail());
        message.setTo(toEmail);
        message.setSubject("Confirm your EduRite account");
        message.setText("""
                Hello %s,
                
                Thanks for signing up for EduRite. Please confirm your email address by opening the link below:
                %s
                
                This verification link expires in %d hours.
                
                If you did not create an EduRite account, you can safely ignore this email.
                """.formatted(recipientName, verificationUrl, expiresInHours));
        try {
            mailSender.send(message);
            log.info("[mail] sent email verification to={}", toEmail);
        } catch (RuntimeException ex) {
            log.error("[mail] failed to send email verification to={}", toEmail, ex);
            throw new EmailDispatchException("We could not send the verification email right now. Please try again.");
        }
    }
}
