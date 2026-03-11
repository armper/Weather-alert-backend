package com.weather.alert.infrastructure.adapter.notification;

import com.weather.alert.domain.model.SmsMessage;
import com.weather.alert.domain.model.SmsSendResult;
import com.weather.alert.domain.port.SmsSenderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.sms.provider", havingValue = "twilio")
public class TwilioSmsSenderAdapter implements SmsSenderPort {

    private final TwilioRestSmsClient twilioRestSmsClient;

    @Override
    public SmsSendResult send(SmsMessage message) {
        return twilioRestSmsClient.send(message);
    }
}
