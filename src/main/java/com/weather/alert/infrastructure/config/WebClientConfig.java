package com.weather.alert.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient noaaWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.weather.gov")
                .defaultHeader("User-Agent", "Weather-Alert-Backend/1.0")
                .build();
    }
}
