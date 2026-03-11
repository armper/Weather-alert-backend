package com.weather.alert.infrastructure.adapter.notification;

import com.twilio.exception.ApiException;
import com.twilio.exception.TwilioException;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.weather.alert.domain.model.DeliveryFailureType;
import com.weather.alert.domain.model.SmsMessage;
import com.weather.alert.domain.model.SmsSendResult;
import com.weather.alert.domain.service.notification.SmsDeliveryException;
import com.weather.alert.infrastructure.config.NotificationSmsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.sms.provider", havingValue = "twilio")
public class TwilioRestSmsClient {

    private final TwilioRestClient twilioRestClient;
    private final NotificationSmsProperties properties;
    private final TwilioMessageFactory messageFactory;

    public SmsSendResult send(SmsMessage message) {
        try {
            MessageCreator creator = messageFactory.create(message, properties);
            Message twilioMessage = creator.create(twilioRestClient);
            return new SmsSendResult(twilioMessage == null ? null : twilioMessage.getSid());
        } catch (IllegalStateException ex) {
            throw new SmsDeliveryException(DeliveryFailureType.NON_RETRYABLE, "Twilio SMS send failed", ex);
        } catch (ApiException ex) {
            throw new SmsDeliveryException(classify(ex), "Twilio SMS send failed", ex);
        } catch (TwilioException ex) {
            throw new SmsDeliveryException(DeliveryFailureType.RETRYABLE, "Twilio SMS send failed", ex);
        }
    }

    private DeliveryFailureType classify(ApiException ex) {
        if (ex.getStatusCode() != null) {
            int statusCode = ex.getStatusCode();
            if (statusCode == 429 || statusCode >= 500) {
                return DeliveryFailureType.RETRYABLE;
            }
            if (statusCode >= 400) {
                return DeliveryFailureType.NON_RETRYABLE;
            }
        }
        return DeliveryFailureType.RETRYABLE;
    }
}
