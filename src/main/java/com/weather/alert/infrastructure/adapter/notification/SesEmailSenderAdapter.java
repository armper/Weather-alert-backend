package com.weather.alert.infrastructure.adapter.notification;

import com.weather.alert.domain.model.DeliveryFailureType;
import com.weather.alert.domain.model.EmailMessage;
import com.weather.alert.domain.model.EmailSendResult;
import com.weather.alert.domain.port.EmailSenderPort;
import com.weather.alert.domain.service.notification.EmailDeliveryException;
import com.weather.alert.infrastructure.config.NotificationEmailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

import java.util.Set;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.email.provider", havingValue = "ses")
public class SesEmailSenderAdapter implements EmailSenderPort {

    private static final Set<String> RETRYABLE_ERROR_CODES = Set.of(
            "Throttling",
            "ThrottlingException",
            "RequestTimeout",
            "ServiceUnavailableException",
            "TooManyRequestsException");

    private final SesClient sesClient;
    private final NotificationEmailProperties properties;

    @Override
    public EmailSendResult send(EmailMessage message) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(properties.getFromAddress())
                    .destination(Destination.builder().toAddresses(message.to()).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(message.subject()).charset("UTF-8").build())
                            .body(Body.builder()
                                    .text(Content.builder().data(message.body() == null ? "" : message.body()).charset("UTF-8").build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            return new EmailSendResult(response.messageId());
        } catch (SesException ex) {
            throw new EmailDeliveryException(classify(ex), "SES email send failed", ex);
        } catch (SdkClientException ex) {
            throw new EmailDeliveryException(DeliveryFailureType.RETRYABLE, "SES client error while sending email", ex);
        }
    }

    private DeliveryFailureType classify(AwsServiceException ex) {
        int statusCode = ex.statusCode();
        if (statusCode >= 500 || statusCode == 429) {
            return DeliveryFailureType.RETRYABLE;
        }
        String errorCode = ex.awsErrorDetails() == null ? null : ex.awsErrorDetails().errorCode();
        if (errorCode != null && RETRYABLE_ERROR_CODES.contains(errorCode)) {
            return DeliveryFailureType.RETRYABLE;
        }
        return DeliveryFailureType.NON_RETRYABLE;
    }
}
