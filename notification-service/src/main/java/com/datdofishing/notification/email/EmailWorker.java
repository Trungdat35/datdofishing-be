package com.datdofishing.notification.email;

import com.datdofishing.notification.config.AppConfig;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Lấy việc từ hàng đợi email.send, dựng nội dung bằng Thymeleaf và gửi SMTP (local: Mailpit).
 * Lỗi -> Spring AMQP thử lại theo cấu hình retry, hết lượt thì message vào email.send.dlq.
 */
@Component
public class EmailWorker {

    private static final Logger log = LoggerFactory.getLogger(EmailWorker.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templates;
    private final JdbcTemplate jdbc;
    private final String from;

    public EmailWorker(JavaMailSender mailSender, TemplateEngine templates, JdbcTemplate jdbc,
                       @Value("${datdofishing.mail-from}") String from) {
        this.mailSender = mailSender;
        this.templates = templates;
        this.jdbc = jdbc;
        this.from = from;
    }

    @RabbitListener(queues = AppConfig.EMAIL_QUEUE)
    public void handle(EmailJob job) throws Exception {
        try {
            Context ctx = new Context(Locale.of("vi", "VN"));
            if (job.variables() != null) {
                ctx.setVariables(job.variables());
            }
            String html = templates.process("mail/" + job.template(), ctx);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(job.to());
            helper.setSubject(job.subject());
            helper.setText(html, true);
            mailSender.send(message);
            log(job, "SENT", null);
            log.info("Đã gửi email '{}' tới {}", job.subject(), job.to());
        } catch (Exception e) {
            log(job, "FAILED", e.getMessage());
            log.warn("Gửi email tới {} lỗi: {}", job.to(), e.getMessage());
            throw e;
        }
    }

    private void log(EmailJob job, String status, String error) {
        try {
            jdbc.update("INSERT INTO email_logs(to_address, subject, template, status, error) VALUES (?, ?, ?, ?, ?)",
                    job.to(), job.subject(), job.template(), status,
                    error == null ? null : error.substring(0, Math.min(error.length(), 1000)));
        } catch (Exception ignored) {
            // Ghi log thất bại không được làm hỏng việc gửi mail
        }
    }
}
