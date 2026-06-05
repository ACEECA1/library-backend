package org.personal.library.dto.badge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BadgeMessage implements Serializable {
    private String actionType; 
    private Long userId;
}
