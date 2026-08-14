package com.bookshop.book;

import com.bookshop.common.NotFoundException;
import com.bookshop.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BookService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("title", "author", "price", "pages", "id");

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public PageResponse<BookDto> list(int page, int size, String search, String sort, String direction) {
        String sortField = SORTABLE_FIELDS.contains(sort) ? sort : "id";
        Sort.Direction sortDirection =
                "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.clamp(size, 1, 50),
                Sort.by(sortDirection, sortField));

        Page<Book> result = (search == null || search.isBlank())
                ? bookRepository.findAll(pageable)
                : bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(
                        search.trim(), search.trim(), pageable);

        return PageResponse.from(result, BookDto::from);
    }

    public BookDto get(Long id) {
        return bookRepository.findById(id)
                .map(BookDto::from)
                .orElseThrow(() -> new NotFoundException("Book %d not found".formatted(id)));
    }
}
