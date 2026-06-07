package org.personal.library.dto.book;

import lombok.Data;
import java.util.List;

@Data
public class BookUpdateRequestDTO {
    private String title;
    private String description;
    private String author;
    private Long seriesId;
    private List<Long> categoryIds;
    private List<Long> tagIds;
}
