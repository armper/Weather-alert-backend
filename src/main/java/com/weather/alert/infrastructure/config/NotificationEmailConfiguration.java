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
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean startTlsEnable,
            @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}") boolean sslEnable,
            @Value("${spring.mail.properties.mail.smtp.ssl.trust:}") String sslTrust,
            @Value("${spring.mail.properties.mail.smtp.connectiontimeout:5000}") int connectionTimeoutMs,
            @Value("${spring.mail.properties.mail.smtp.timeout:5000}") int timeoutMs,
            @Value("${spring.mail.properties.mail.smtp.writetimeout:5000}") int writeTimeoutMs) {
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
        javaMailProps.put("mail.smtp.ssl.enable", String.valueOf(sslEnable));
        if (sslTrust != null && !sslTrust.isBlank()) {
            javaMailProps.put("mail.smtp.ssl.trust", sslTrust);
        }
        javaMailProps.put("mail.smtp.connectiontimeout", String.valueOf(connectionTimeoutMs));
        javaMailProps.put("mail.smtp.timeout", String.valueOf(timeoutMs));
        javaMailProps.put("mail.smtp.writetimeout", String.valueOf(writeTimeoutMs));
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
