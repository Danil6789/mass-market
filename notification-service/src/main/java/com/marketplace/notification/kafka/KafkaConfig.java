package com.marketplace.notification.kafka;

import com.marketplace.common.constant.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumer-side Kafka configuration.
 *
 * <p>Uses {@link JsonMessageConverter} + {@link ByteArrayDeserializer} so that
 * deserialization is driven by the listener method parameter type
 * (e.g. {@code void onOrderCreated(OrderCreatedEvent event)}). This avoids
 * the need for {@code __TypeId__} headers in Kafka, which Spring Boot's
 * {@code spring.kafka.producer.properties} map doesn't reliably propagate.</p>
 *
 * <p>{@link ErrorHandlingDeserializer} wraps the raw deserializers so that
 * any {@code SerializationException} (from corrupt or stale DLT messages)
 * is captured and routed through the {@link DefaultErrorHandler} instead
 * of crashing the listener container.</p>
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

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

    /**
     * Custom consumer factory backed by {@link ByteArrayDeserializer}. The
     * {@link JsonMessageConverter} (registered below) is responsible for
     * turning the raw bytes into the typed payload matching the listener
     * method signature.
     */
    @Bean
    public ConsumerFactory<String, byte[]> kafkaConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put("spring.deserializer.key.delegate.class", StringDeserializer.class.getName());
        props.put("spring.deserializer.value.delegate.class", ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Container factory wired to the consumer factory above and the
     * {@link JsonMessageConverter}. {@code @KafkaListener(containerFactory = "kafkaListenerContainerFactory")}
     * uses this for type-aware deserialization. We let
     * {@code @RetryableTopic} handle retries via its own non-blocking error
     * handler; a fixed back-off here would short-circuit that.
     */
    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory(
            ConsumerFactory<String, byte[]> kafkaConsumerFactory,
            JsonMessageConverter jsonMessageConverter
    ) {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaConsumerFactory);
        factory.setRecordMessageConverter(jsonMessageConverter);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean
    public JsonMessageConverter jsonMessageConverter() {
        return new JsonMessageConverter();
    }
}
