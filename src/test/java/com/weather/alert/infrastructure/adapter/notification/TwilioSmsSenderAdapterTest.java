package com.weather.alert.infrastructure.adapter.notification;

import com.weather.alert.domain.model.SmsMessage;
import com.weather.alert.domain.model.SmsSendResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwilioSmsSenderAdapterTest {

    @Mock
    private TwilioRestSmsClient twilioRestSmsClient;

    @InjectMocks
    private TwilioSmsSenderAdapter adapter;

    @Test
    void shouldDelegateSmsSendToTwilioClient() {
        SmsMessage message = SmsMessage.builder()
                .to("+14075550199")
                .body("Storm alert")
                .build();
        when(twilioRestSmsClient.send(message)).thenReturn(new SmsSendResult("SM123"));

        SmsSendResult result = adapter.send(message);

        assertEquals("SM123", result.providerMessageId());
        verify(twilioRestSmsClient).send(message);
    }
}
