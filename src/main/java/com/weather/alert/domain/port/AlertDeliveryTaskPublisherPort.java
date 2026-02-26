package com.weather.alert.domain.port;

public interface AlertDeliveryTaskPublisherPort {

    void publishTask(String deliveryId);
}
