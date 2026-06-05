package org.personal.library.model;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Series extends BaseEntity {

    private String name;
    private String description;

    @OneToMany(mappedBy = "series")
    private List<Book> books = new ArrayList<>();
}
