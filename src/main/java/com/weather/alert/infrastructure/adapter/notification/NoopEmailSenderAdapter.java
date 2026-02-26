package com.weather.alert.infrastructure.adapter.notification;

import com.weather.alert.domain.model.EmailMessage;
import com.weather.alert.domain.model.EmailSendResult;
import com.weather.alert.domain.port.EmailSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.notification.email.provider", havingValue = "noop")
@Slf4j
public class NoopEmailSenderAdapter implements EmailSenderPort {

    @Override
    public EmailSendResult send(EmailMessage message) {
        log.info("Noop email sender active. Skipping email delivery to {}", message == null ? "unknown" : message.to());
        return new EmailSendResult(null);
    }
}
