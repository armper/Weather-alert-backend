package com.weather.alert.infrastructure.adapter.notification;

import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import com.weather.alert.domain.model.SmsMessage;
import com.weather.alert.infrastructure.config.NotificationSmsProperties;
import org.springframework.stereotype.Component;

@Component
public class TwilioMessageFactory {

    public MessageCreator create(SmsMessage message, NotificationSmsProperties properties) {
        PhoneNumber to = new PhoneNumber(message.to());
        String body = message.body() == null ? "" : message.body();

        if (properties.getMessagingServiceSid() != null && !properties.getMessagingServiceSid().isBlank()) {
            return com.twilio.rest.api.v2010.account.Message.creator(
                    to,
                    properties.getMessagingServiceSid().trim(),
                    body);
        }

        if (properties.getFromNumber() != null && !properties.getFromNumber().isBlank()) {
            return com.twilio.rest.api.v2010.account.Message.creator(
                    to,
                    new PhoneNumber(properties.getFromNumber().trim()),
                    body);
        }

        throw new IllegalStateException("Twilio SMS requires either a from number or a messaging service SID");
    }
}
