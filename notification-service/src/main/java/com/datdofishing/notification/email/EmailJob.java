package com.datdofishing.notification.email;

import java.util.Map;

/** Một việc gửi email nằm trong hàng đợi RabbitMQ. */
public record EmailJob(String to, String subject, String template, Map<String, Object> variables) {
}
