package com.bookshop.book;

import java.math.BigDecimal;

/** Read model exposed by the catalog API; decoupled from the JPA entity. */
public record BookDto(
        Long id,
        String title,
        String author,
        String description,
        int pages,
        String imageUrl,
        BigDecimal price,
        int stock) {

    public static BookDto from(Book book) {
        return new BookDto(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getDescription(),
                book.getPages(),
                book.getImageUrl(),
                book.getPrice(),
                book.getStock());
    }
}
