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
    private String note;
    private LocalDateTime createdAt;
}
