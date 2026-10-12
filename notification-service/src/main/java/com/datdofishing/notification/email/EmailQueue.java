package com.datdofishing.notification.email;

import com.datdofishing.notification.config.AppConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Đẩy việc gửi email vào RabbitMQ thay vì gửi trực tiếp, để SMTP chậm/lỗi không ảnh hưởng luồng chính. */
@Component
public class EmailQueue {

    private final RabbitTemplate rabbit;

    public EmailQueue(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public void enqueue(EmailJob job) {
        if (job.to() == null || job.to().isBlank()) {
            return;
        }
        rabbit.convertAndSend(AppConfig.EMAIL_EXCHANGE, AppConfig.EMAIL_QUEUE, job);
    }
}
