package com.weather.alert.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.notification.sms")
@Data
public class NotificationSmsProperties {

    private String provider = "noop";
    private String fromNumber;
    private String messagingServiceSid;
    private Twilio twilio = new Twilio();

    public boolean hasMessagingServiceSid() {
        return messagingServiceSid != null && !messagingServiceSid.isBlank();
    }

    public boolean hasFromNumber() {
        return fromNumber != null && !fromNumber.isBlank();
    }

    @Data
    public static class Twilio {
        private String accountSid;
        private String authToken;
        private String apiKey;
        private String apiSecret;

        public boolean hasApiKeyCredentials() {
            return apiKey != null && !apiKey.isBlank()
                    && apiSecret != null && !apiSecret.isBlank()
                    && accountSid != null && !accountSid.isBlank();
        }

        public boolean hasAuthTokenCredentials() {
            return accountSid != null && !accountSid.isBlank()
                    && authToken != null && !authToken.isBlank();
        }
    }
}
