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

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseEntity {

    @Column(unique = true, nullable = false)
    @KeywordField
    private String name;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToMany(mappedBy = "categories")
    private Set<Book> books = new HashSet<>();
}
