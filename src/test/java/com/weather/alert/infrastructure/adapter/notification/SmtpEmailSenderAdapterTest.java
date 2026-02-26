package com.weather.alert.infrastructure.adapter.notification;

import com.weather.alert.domain.model.DeliveryFailureType;
import com.weather.alert.domain.model.EmailMessage;
import com.weather.alert.domain.model.EmailSendResult;
import com.weather.alert.domain.service.notification.EmailDeliveryException;
import com.weather.alert.infrastructure.config.NotificationEmailProperties;
import jakarta.mail.Session;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpEmailSenderAdapterTest {

    private JavaMailSender mailSender;
    private SmtpEmailSenderAdapter adapter;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        NotificationEmailProperties properties = new NotificationEmailProperties();
        properties.setFromAddress("no-reply@weather-alert.local");
        adapter = new SmtpEmailSenderAdapter(mailSender, properties);
    }

    @Test
    void shouldSendEmailUsingJavaMailSender() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        EmailSendResult result = adapter.send(EmailMessage.builder()
                .to("user@example.com")
                .subject("Weather alert")
                .body("Rain expected")
                .build());

        assertNotNull(result);
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("Weather alert", captor.getValue().getSubject());
    }

    @Test
    void shouldClassifyAuthenticationFailuresAsNonRetryable() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailAuthenticationException("bad credentials")).when(mailSender).send(any(MimeMessage.class));

        EmailDeliveryException ex = assertThrows(
                EmailDeliveryException.class,
                () -> adapter.send(EmailMessage.builder()
                        .to("user@example.com")
                        .subject("Weather alert")
                        .body("Rain expected")
                        .build()));

        assertEquals(DeliveryFailureType.NON_RETRYABLE, ex.getFailureType());
    }

    @Test
    void shouldClassifySendFailedExceptionAsNonRetryable() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        MailSendException sendException = new MailSendException(
                "send failed",
                null,
                Map.of("msg", new SendFailedException("invalid recipient")));
        doThrow(sendException).when(mailSender).send(any(MimeMessage.class));

        EmailDeliveryException ex = assertThrows(
                EmailDeliveryException.class,
                () -> adapter.send(EmailMessage.builder()
                        .to("user@example.com")
                        .subject("Weather alert")
                        .body("Rain expected")
                        .build()));

        assertEquals(DeliveryFailureType.NON_RETRYABLE, ex.getFailureType());
    }

    @Test
    void shouldClassifyGenericMailSendExceptionAsRetryable() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("smtp timeout")).when(mailSender).send(any(MimeMessage.class));

        EmailDeliveryException ex = assertThrows(
                EmailDeliveryException.class,
                () -> adapter.send(EmailMessage.builder()
                        .to("user@example.com")
                        .subject("Weather alert")
                        .body("Rain expected")
                        .build()));

        assertEquals(DeliveryFailureType.RETRYABLE, ex.getFailureType());
    }
}
