package com.marketplace.notification.kafka;

import com.marketplace.common.constant.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Consumer-side Kafka configuration.
 *
 * <p>This service is consumer-only — there is no {@link org.springframework.kafka.core.ProducerFactory}
 * or {@link org.springframework.kafka.core.KafkaTemplate}. The consumer factory
 * is auto-configured by Spring Boot from {@code spring.kafka.consumer.*} in
 * {@code application.yml} (group-id {@code notification-group}).</p>
 *
 * <p>The {@link NewTopic} beans below trigger automatic topic creation at
 * startup. Three partitions, replication factor 1 (single-broker dev).</p>
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name(KafkaTopics.USER_REGISTERED)
                .partitions(KafkaTopics.DEFAULT_PARTITIONS)
                .replicas(KafkaTopics.DEFAULT_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_CREATED)
                .partitions(KafkaTopics.DEFAULT_PARTITIONS)
                .replicas(KafkaTopics.DEFAULT_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic orderPaidTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_PAID)
                .partitions(KafkaTopics.DEFAULT_PARTITIONS)
                .replicas(KafkaTopics.DEFAULT_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_CANCELLED)
                .partitions(KafkaTopics.DEFAULT_PARTITIONS)
                .replicas(KafkaTopics.DEFAULT_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic productDeletedTopic() {
        return TopicBuilder.name(KafkaTopics.PRODUCT_DELETED)
                .partitions(KafkaTopics.DEFAULT_PARTITIONS)
                .replicas(KafkaTopics.DEFAULT_REPLICATION_FACTOR)
                .build();
    }
}
