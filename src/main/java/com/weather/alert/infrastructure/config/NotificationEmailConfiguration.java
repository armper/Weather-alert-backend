package com.weather.alert.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

import java.util.Properties;

@Configuration
public class NotificationEmailConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.notification.email.provider", havingValue = "smtp", matchIfMissing = true)
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender(
            @Value("${spring.mail.host:localhost}") String host,
            @Value("${spring.mail.port:1025}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${spring.mail.properties.mail.smtp.auth:false}") boolean smtpAuth,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean startTlsEnable) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        if (username != null && !username.isBlank()) {
            sender.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            sender.setPassword(password);
        }
        Properties javaMailProps = sender.getJavaMailProperties();
        javaMailProps.put("mail.smtp.auth", String.valueOf(smtpAuth));
        javaMailProps.put("mail.smtp.starttls.enable", String.valueOf(startTlsEnable));
        return sender;
    }

    @Bean
    @ConditionalOnProperty(name = "app.notification.email.provider", havingValue = "ses")
    public SesClient sesClient(NotificationEmailProperties properties) {
        return SesClient.builder()
                .region(Region.of(properties.getSes().getRegion()))
                .build();
    }
}
