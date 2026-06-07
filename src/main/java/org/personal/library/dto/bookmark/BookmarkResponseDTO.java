package org.personal.library.dto.bookmark;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BookmarkResponseDTO {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookThumbnailPath;
    private String bookAuthor;
    private Double bookAverageRating;
    private String bookUploaderUsername;
    private Long bookViews;
    private String note;
    private LocalDateTime createdAt;
}
