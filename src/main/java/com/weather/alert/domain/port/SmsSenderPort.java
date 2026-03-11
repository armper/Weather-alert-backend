package com.weather.alert.domain.port;

import com.weather.alert.domain.model.SmsMessage;
import com.weather.alert.domain.model.SmsSendResult;

public interface SmsSenderPort {

    SmsSendResult send(SmsMessage message);
}
