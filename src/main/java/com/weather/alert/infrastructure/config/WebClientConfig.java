package com.weather.alert.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient noaaWebClient(@Value("${app.noaa.max-in-memory-size:10MB}") DataSize maxInMemorySize) {
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize((int) maxInMemorySize.toBytes()))
                .build();

        return WebClient.builder()
                .baseUrl("https://api.weather.gov")
                .defaultHeader("User-Agent", "Weather-Alert-Backend/1.0")
                .exchangeStrategies(exchangeStrategies)
                .build();
    }
}
