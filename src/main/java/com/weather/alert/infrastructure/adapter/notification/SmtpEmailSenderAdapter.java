package com.weather.alert.infrastructure.adapter.notification;

import com.weather.alert.domain.model.DeliveryFailureType;
import com.weather.alert.domain.model.EmailMessage;
import com.weather.alert.domain.model.EmailSendResult;
import com.weather.alert.domain.port.EmailSenderPort;
import com.weather.alert.domain.service.notification.EmailDeliveryException;
import com.weather.alert.infrastructure.config.NotificationEmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.email.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;
    private final NotificationEmailProperties properties;

    @Override
    public EmailSendResult send(EmailMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(properties.getFromAddress());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.body() == null ? "" : message.body(), false);

            mailSender.send(mimeMessage);
            return new EmailSendResult(resolveMessageId(mimeMessage));
        } catch (MailAuthenticationException | MailParseException | MailPreparationException ex) {
            throw new EmailDeliveryException(DeliveryFailureType.NON_RETRYABLE, "SMTP email send failed", ex);
        } catch (MailSendException ex) {
            throw new EmailDeliveryException(classify(ex), "SMTP email send failed", ex);
        } catch (MailException | MessagingException ex) {
            throw new EmailDeliveryException(DeliveryFailureType.RETRYABLE, "SMTP email send failed", ex);
        }
    }

    private DeliveryFailureType classify(MailSendException ex) {
        if (ex.getFailedMessages() != null) {
            boolean hasNonRetryable = ex.getFailedMessages().values().stream()
                    .anyMatch(cause -> cause instanceof SendFailedException);
            if (hasNonRetryable) {
                return DeliveryFailureType.NON_RETRYABLE;
            }
        }
        return DeliveryFailureType.RETRYABLE;
    }

    private String resolveMessageId(MimeMessage mimeMessage) throws MessagingException {
        String[] messageIds = mimeMessage.getHeader("Message-ID");
        if (messageIds == null || messageIds.length == 0) {
            return null;
        }
        return messageIds[0];
    }
}
