package com.mpie.service2.configuration;

import com.mpie.service2.model.Book;
import com.mpie.service2.model.BookAvailabilityStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {

    public static final List<String> ALLOWED_CATEGORIES = List.of("Fantasy", "Science-Fiction", "Naukowe");

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServer;

    @Value("${service2.group.id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, Book> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        JsonDeserializer<Book> deserializer = new JsonDeserializer<>(Book.class);
        deserializer.setUseTypeMapperForKey(true);
        deserializer.addTrustedPackages("*");

        ErrorHandlingDeserializer<Book> errorHandlingDeserializer = new ErrorHandlingDeserializer<>(deserializer);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandlingDeserializer);
    }

    @Bean
    public DefaultErrorHandler errorHandler() {
        FixedBackOff backOff = new FixedBackOff(1000L, 1);

        return new DefaultErrorHandler((record, exception) -> {
            log.error("Record skipped and sent to DLT: {}. Error: {}", record.value(), exception.getMessage());
        }, backOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Book> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Book> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler());

        factory.setBatchListener(true);

        factory.setRecordFilterStrategy(new RecordFilterStrategy<String, Book>() {
            @Override
            public boolean filter(ConsumerRecord<String, Book> consumerRecord) {
                Book book = consumerRecord.value();

                if (!ALLOWED_CATEGORIES.contains(book.getCategory())) {
                    log.warn("Book [{}] has unsupported category: {}", book.getTitle(), book.getCategory());
                    throw new IllegalArgumentException("Wrong book category. Will not be persisted");
                }
                return false;
            }
        });
        return factory;
    }
}