package com.weather.alert.infrastructure.config;

import com.twilio.http.TwilioRestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationSmsConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.notification.sms.provider", havingValue = "twilio")
    public TwilioRestClient twilioRestClient(NotificationSmsProperties properties) {
        NotificationSmsProperties.Twilio twilio = properties.getTwilio();
        if (twilio.hasApiKeyCredentials()) {
            return new TwilioRestClient.Builder(
                    twilio.getApiKey(),
                    twilio.getApiSecret())
                    .accountSid(twilio.getAccountSid())
                    .build();
        }
        if (twilio.hasAuthTokenCredentials()) {
            return new TwilioRestClient.Builder(
                    twilio.getAccountSid(),
                    twilio.getAuthToken())
                    .build();
        }
        throw new IllegalStateException("Twilio SMS requires either accountSid/authToken or accountSid/apiKey/apiSecret");
    }
}
