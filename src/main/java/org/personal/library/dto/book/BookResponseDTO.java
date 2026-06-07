package org.personal.library.dto.book;

import lombok.Builder;
import lombok.Data;
import org.personal.library.model.Book.BookStatus;

import java.time.LocalDateTime;
import java.util.List;

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
    private boolean bookmarked;
    private Long userBookmarkId;
    private String uploaderUsername;
    private LocalDateTime createdAt;
    private List<String> categories;
    private List<String> tags;
}
