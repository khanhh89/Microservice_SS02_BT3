package com.librax.bookservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model đại diện cho một đầu sách trong thư viện LibraX.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String category;

    /** true = còn sách có thể mượn, false = đang cho mượn hết */
    private boolean available;
}
