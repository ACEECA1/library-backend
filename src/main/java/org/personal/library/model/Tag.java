package org.personal.library.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tag extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToMany(mappedBy = "tags")
    private Set<Book> books = new HashSet<>();
}
