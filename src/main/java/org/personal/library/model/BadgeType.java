package org.personal.library.model;

public enum BadgeType {
    UPLOADER_BRONZE("Uploaded 1 Book"),
    UPLOADER_SILVER("Uploaded 10 Books"),
    UPLOADER_GOLD("Uploaded 50 Books"),

    REVIEWER_BRONZE("Reviewed 1 Book"),
    REVIEWER_SILVER("Reviewed 10 Books"),
    REVIEWER_GOLD("Reviewed 50 Books"),

    POPULAR_COMMENTER("Received 10 Upvotes on a comment");

    private final String description;

    BadgeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
