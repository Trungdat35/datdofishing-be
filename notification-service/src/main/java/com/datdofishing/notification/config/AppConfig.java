package com.datdofishing.notification.config;

import com.datdofishing.common.security.ResourceServerSecurity;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.datdofishing.notification.sse.SseHub;

@Configuration
public class AppConfig {

    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_QUEUE = "email.send";
    public static final String EMAIL_DLX = "email.dlx";
    public static final String EMAIL_DLQ = "email.send.dlq";
    public static final String REDIS_CHANNEL = "datdofishing:notifications";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return ResourceServerSecurity.build(http, new String[]{}, new String[]{});
    }

    // ---------- RabbitMQ: hàng đợi email + dead-letter queue ----------
    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange emailDeadLetterExchange() {
        return new DirectExchange(EMAIL_DLX, true, false);
    }

    @Bean
    public Queue emailSendQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ)
                .build();
    }

    @Bean
    public Queue emailDeadLetterQueue() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailSendQueue()).to(emailExchange()).with(EMAIL_QUEUE);
    }

    @Bean
    public Binding emailDeadLetterBinding() {
        return BindingBuilder.bind(emailDeadLetterQueue()).to(emailDeadLetterExchange()).with(EMAIL_DLQ);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter("com.datdofishing.*");
    }

    // ---------- Redis pub/sub: phát thông báo tới mọi instance đang giữ kết nối SSE ----------
    @Bean
    public RedisMessageListenerContainer redisListenerContainer(RedisConnectionFactory factory, SseHub hub) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        MessageListenerAdapter adapter = new MessageListenerAdapter(hub, "onRedisMessage");
        adapter.afterPropertiesSet();
        container.addMessageListener(adapter, new ChannelTopic(REDIS_CHANNEL));
        return container;
    }
}
