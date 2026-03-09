package com.weather.alert.infrastructure.adapter.notification;

import com.weather.alert.application.usecase.ProcessAlertDeliveryTaskUseCase;
import com.weather.alert.domain.port.AlertDeliveryTaskPublisherPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Slf4j
public class InProcessAlertDeliveryTaskPublisherAdapter implements AlertDeliveryTaskPublisherPort {

    private final ProcessAlertDeliveryTaskUseCase processAlertDeliveryTaskUseCase;
    private final TaskExecutor taskExecutor;

    @Autowired
    public InProcessAlertDeliveryTaskPublisherAdapter(
            ProcessAlertDeliveryTaskUseCase processAlertDeliveryTaskUseCase,
            @Qualifier("alertDeliveryTaskExecutor") TaskExecutor taskExecutor) {
        this.processAlertDeliveryTaskUseCase = processAlertDeliveryTaskUseCase;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void publishTask(String deliveryId) {
        if (deliveryId == null || deliveryId.isBlank()) {
            return;
        }
        Runnable work = () -> {
            try {
                processAlertDeliveryTaskUseCase.processTask(deliveryId);
            } catch (Exception ex) {
                log.error("Failed to process delivery task {}", deliveryId, ex);
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    taskExecutor.execute(work);
                }
            });
            return;
        }

        taskExecutor.execute(work);
    }
}
