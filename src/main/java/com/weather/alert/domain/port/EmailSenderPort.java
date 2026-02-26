package com.weather.alert.domain.port;

import com.weather.alert.domain.model.EmailMessage;
import com.weather.alert.domain.model.EmailSendResult;

public interface EmailSenderPort {

    EmailSendResult send(EmailMessage message);
}
