package com.weather.alert.infrastructure.adapter.notification;

import com.weather.alert.domain.model.DeliveryFailureType;
import com.weather.alert.domain.model.EmailMessage;
import com.weather.alert.domain.model.EmailSendResult;
import com.weather.alert.domain.service.notification.EmailDeliveryException;
import com.weather.alert.infrastructure.config.NotificationEmailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SesEmailSenderAdapterTest {

    private SesClient sesClient;
    private SesEmailSenderAdapter adapter;

    @BeforeEach
    void setUp() {
        sesClient = mock(SesClient.class);
        NotificationEmailProperties properties = new NotificationEmailProperties();
        properties.setFromAddress("no-reply@weather-alert.local");
        adapter = new SesEmailSenderAdapter(sesClient, properties);
    }

    @Test
    void shouldSendEmailViaSes() {
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("ses-message-1").build());

        EmailSendResult result = adapter.send(EmailMessage.builder()
                .to("user@example.com")
                .subject("Weather alert")
                .body("Rain expected")
                .build());

        assertEquals("ses-message-1", result.providerMessageId());
        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void shouldClassifyServiceUnavailableAsRetryable() {
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenThrow(
                SesException.builder()
                        .statusCode(503)
                        .awsErrorDetails(AwsErrorDetails.builder()
                                .errorCode("ServiceUnavailableException")
                                .errorMessage("service unavailable")
                                .build())
                        .build());

        EmailDeliveryException ex = assertThrows(
                EmailDeliveryException.class,
                () -> adapter.send(EmailMessage.builder()
                        .to("user@example.com")
                        .subject("Weather alert")
                        .body("Rain expected")
                        .build()));

        assertEquals(DeliveryFailureType.RETRYABLE, ex.getFailureType());
    }

    @Test
    void shouldClassifyMessageRejectedAsNonRetryable() {
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenThrow(
                SesException.builder()
                        .statusCode(400)
                        .awsErrorDetails(AwsErrorDetails.builder()
                                .errorCode("MessageRejected")
                                .errorMessage("email address is not verified")
                                .build())
                        .build());

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
    void shouldClassifySdkClientExceptionAsRetryable() {
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(SdkClientException.builder().message("connection reset").build());

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
