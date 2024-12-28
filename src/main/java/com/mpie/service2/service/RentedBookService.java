package com.mpie.service2.service;

import com.mpie.service2.mapper.BookMapper;
import com.mpie.service2.model.Book;
import com.mpie.service2.model.BookAvailabilityStatus;
import com.mpie.service2.model.BookDto;
import com.mpie.service2.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RentedBookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    @KafkaListener(topics = "rented-books", groupId = "book-rented-group", containerFactory = "kafkaListenerContainerFactory")
    public void listen(Book book) {
        try {
            log.info("Received book event: " + book);
            if (BookAvailabilityStatus.RENTED.equals(book.getBookStatus())) {
                saveRentedBook(book);
            } else if (BookAvailabilityStatus.AVAILABLE.equals(book.getBookStatus())) {
                handleReturnedBook(book);
            } else {
                throw new IllegalArgumentException("Unsupported book status: " + book.getBookStatus());
            }
        } catch (Exception e) {
            log.error("Error during processing book event: {}", book, e);
            handleDltBook(book);
            throw e;
        }
    }

    @DltHandler
    public void handleDltBook(Book book) {
        log.info("Event on dlt, payload={}", book);
    }

    public List<BookDto> getRentedBooks(Pageable pageable) {
        return bookMapper.toDtos(bookRepository.findAll(pageable).getContent());
    }

    private void handleReturnedBook(Book returnedBook) {
        bookRepository.findById(returnedBook.getIsbn()).ifPresent(book -> {
            bookRepository.delete(book);
            log.info("Book with isbn {} removed from rented books.", returnedBook.getIsbn());
        });
    }

    private void saveRentedBook(Book bookRented) {
        bookRepository.findById(bookRented.getIsbn()).ifPresentOrElse(
                book -> {
                    book.setBorrower(bookRented.getBorrower());
                    bookRepository.save(book);
                },
                () -> bookRepository.save(bookRented)
        );
    }
}
