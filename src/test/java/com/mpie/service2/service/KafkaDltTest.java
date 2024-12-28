//package com.mpie.service2.service;
//
//import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
//import com.mpie.service2.model.Book;
//import com.mpie.service2.model.BookAvailabilityStatus;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.kafka.test.utils.KafkaTestUtils;
//import org.springframework.kafka.test.EmbeddedKafkaBroker;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//
//@EmbeddedKafka(partitions = 1, topics = {"rented-books", "rented-books-dlt"})
//public class KafkaDltTest {
//
//    @Autowired
//    private KafkaTemplate<String, Book> kafkaTemplate;
//
//    @Autowired
//    private EmbeddedKafkaBroker embeddedKafkaBroker;
//
//    @Test
//    public void testDeadLetterTopic() throws Exception {
//        // Wysyłamy wiadomość na główny temat, która powinna trafić na DLT
//        String message = "Test message for DLT";
//        kafkaTemplate.send("rented-books", message);
//
//        // Sprawdzamy, czy wiadomość trafiła do DLT
//        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(
//                embeddedKafkaBroker.(StringDeserializer.class, StringDeserializer.class);
//                "rented-books-dlt", 5000);
//
//        assertNotNull(record);
//        assertEquals(message, record.value());
//    }
//}
