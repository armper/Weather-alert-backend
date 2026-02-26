package com.weather.alert.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class NotificationEmailConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.notification.email.provider", havingValue = "ses")
    public SesClient sesClient(NotificationEmailProperties properties) {
        return SesClient.builder()
                .region(Region.of(properties.getSes().getRegion()))
                .build();
    }
}
