package org.personal.library.dto.book;

import lombok.Builder;
import lombok.Data;
import org.personal.library.model.Book.BookStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class BookResponseDTO {
    private Long id;
    private String title;
    private String author;
    private String description;
    private String thumbnailPath;
    private BookStatus status;
    private long views;
    private double averageRating;
    private int reviewCount;
    private boolean isBookmarked;
    private Long userBookmarkId;
    private String uploaderUsername;
    private LocalDateTime createdAt;
}
