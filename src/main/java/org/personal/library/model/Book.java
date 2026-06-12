package org.personal.library.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.engine.backend.types.Sortable;

import java.util.HashSet;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Indexed
public class Book extends BaseEntity {

    @Column(nullable = false)
    @FullTextField(analyzer = "english")
    private String title;

    @Column(length = 2000)
    @FullTextField(analyzer = "english")
    private String description;

    @Column
    private String extractedTextPath;

    @Transient
    @FullTextField(analyzer = "english")
    @FullTextField(name = "content_fr", analyzer = "french")
    @FullTextField(name = "content_es", analyzer = "spanish")
    @FullTextField(name = "content_de", analyzer = "german")
    @IndexingDependency(derivedFrom = {
            @ObjectPath(@PropertyValue(propertyName = "extractedTextPath"))
    })
    public String getContent() {
        if (extractedTextPath == null || extractedTextPath.isEmpty()) {
            return null;
        }
        try {
            Path path = Paths.get(extractedTextPath);
            if (Files.exists(path)) {
                return Files.readString(path);
            }
        } catch (Exception e) {
            log.error("Failed to read extracted text from {}", extractedTextPath, e);
        }
        return null;
    }

    @Column(nullable = false)
    private String pdfFilePath;

    @Column
    private String thumbnailPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @KeywordField
    private BookStatus status = BookStatus.PENDING;

    @Column(nullable = false)
    @GenericField(sortable = Sortable.YES)
    private long views = 0;

    @Column(nullable = false)
    @GenericField(sortable = Sortable.YES)
    private double averageRating = 0.0;

    @Column(nullable = false)
    private int reviewCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private User uploader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    @IndexedEmbedded
    private Series series;

    @ManyToMany
    @JoinTable(
            name = "book_categories",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @IndexedEmbedded
    private Set<Category> categories = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "book_tags",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @IndexedEmbedded
    private Set<Tag> tags = new HashSet<>();

    @Column
    @FullTextField(analyzer = "english")
    private String author;

    public enum BookStatus {
        PENDING, LIVE, DELETED
    }
}
