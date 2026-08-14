package com.bookshop.book;

import com.bookshop.common.NotFoundException;
import com.bookshop.common.PageResponse;
import com.bookshop.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BookService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("title", "author", "price", "pages", "id");

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Cacheable(cacheNames = CacheConfig.BOOKS_PAGE_CACHE,
            key = "#page + ':' + #size + ':' + (#search ?: '') + ':' + #sort + ':' + #direction")
    public PageResponse<BookDto> list(int page, int size, String search, String sort, String direction) {
        String sortField = Optional.ofNullable(sort)
                .filter(SORTABLE_FIELDS::contains)
                .orElse("id");
        Sort.Direction sortDirection = Optional.ofNullable(direction)
                .filter("desc"::equalsIgnoreCase)
                .map(ignored -> Sort.Direction.DESC)
                .orElse(Sort.Direction.ASC);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.clamp(size, 1, 50),
                Sort.by(sortDirection, sortField));

        Page<Book> result = Optional.ofNullable(search)
                .map(String::trim)
                .filter(term -> !term.isEmpty())
                .map(term -> bookRepository
                        .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(term, term, pageable))
                .orElseGet(() -> bookRepository.findAll(pageable));

        return PageResponse.from(result, BookDto::from);
    }

    @Cacheable(cacheNames = CacheConfig.BOOK_CACHE, key = "#id")
    public BookDto get(Long id) {
        return bookRepository.findById(id)
                .map(BookDto::from)
                .orElseThrow(() -> new NotFoundException("Book %d not found".formatted(id)));
    }
}
