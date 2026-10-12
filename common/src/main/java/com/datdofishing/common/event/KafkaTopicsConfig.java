package com.datdofishing.common.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/** Tạo sẵn các topic với 3 partition để nhiều instance order-service chia nhau xử lý. */
@Configuration
@ConditionalOnProperty(name = "datdofishing.kafka.create-topics", havingValue = "true")
public class KafkaTopicsConfig {

    @Bean
    public KafkaAdmin.NewTopics datdofishingTopics() {
        NewTopic[] topics = new NewTopic[Topics.ALL.length];
        for (int i = 0; i < Topics.ALL.length; i++) {
            topics[i] = TopicBuilder.name(Topics.ALL[i]).partitions(3).replicas(1).build();
        }
        return new KafkaAdmin.NewTopics(topics);
    }
}
