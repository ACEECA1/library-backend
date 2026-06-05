package org.personal.library.dto.progress;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReadingProgressDTO {
    private Long bookId;
    private int lastPageRead;
    private LocalDateTime lastReadAt;
}
