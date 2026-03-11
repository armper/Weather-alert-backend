package com.weather.alert.infrastructure.adapter.notification;

import com.weather.alert.domain.model.SmsMessage;
import com.weather.alert.domain.model.SmsSendResult;
import com.weather.alert.domain.port.SmsSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.notification.sms.provider", havingValue = "noop")
@Slf4j
public class NoopSmsSenderAdapter implements SmsSenderPort {

    @Override
    public SmsSendResult send(SmsMessage message) {
        log.info("Noop SMS sender active. Skipping SMS delivery to {}", message == null ? "unknown" : message.to());
        return new SmsSendResult(null);
    }
}
