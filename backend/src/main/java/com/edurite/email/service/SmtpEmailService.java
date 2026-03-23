package com.edurite.email.service;

import com.edurite.auth.config.MailSenderProperties;
import com.edurite.auth.exception.EmailDispatchException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailSenderProperties.senderEmail());
            helper.setTo(toEmail);
            helper.setSubject("Confirm your EduRite account");
            helper.setText(buildVerificationEmailBody(recipientName, verificationUrl, expiresInHours), true);

            mailSender.send(message);
            log.info("[mail] sent email verification to={}", toEmail);
        } catch (MessagingException | RuntimeException ex) {
            log.error("[mail] failed to send email verification to={}", toEmail, ex);
            throw new EmailDispatchException("We could not send the verification email right now. Please try again.");
        }
    }

    private String buildVerificationEmailBody(String recipientName, String verificationUrl, long expiresInHours) {
        String safeRecipientName = recipientName == null || recipientName.isBlank() ? "there" : recipientName;
        return """
                <p>Hello %s,</p>
                <p>Thanks for signing up for EduRite. Please confirm your email address by opening the link below:</p>
                <p><a href=\"%s\">Verify your email address</a></p>
                <p>If the button does not work, copy and paste this URL into your browser:</p>
                <p>%s</p>
                <p>This verification link expires in %d hours.</p>
                <p>If you did not create an EduRite account, you can safely ignore this email.</p>
                """.formatted(safeRecipientName, verificationUrl, verificationUrl, expiresInHours);
    }
}
