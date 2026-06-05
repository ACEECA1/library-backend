package org.personal.library.dto.bookmark;

import lombok.Data;

@Data
public class BookmarkRequestDTO {
    private Long bookId;
    private Integer pageNumber; // optional
    private String note; // optional
}
