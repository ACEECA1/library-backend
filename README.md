# Digital Library Management System

A robust, enterprise-grade RESTful API backend for managing a digital library of PDF books. Built with Spring Boot 3, Java 17, and MySQL, this system offers a comprehensive suite of features for administrators and users.

## Core Features

- **Authentication & Security**
  - Secure JWT-based authentication and role-based and permission-based access control hybrid.
  - Password reset workflows, account banning, timeouts, and session invalidation.

- **Book Management & Processing**
  - Upload PDF books with automatic virus scanning via ClamAV integration.
  - Automatic thumbnail generation from PDF title pages using Apache PDFBox.
  - Asynchronous background text extraction and Lucene indexing for fast, full-text searching.

- **Content Organization & Searching**
  - Metadata management including Categories, Tags, and Series.
  - Advanced search and filtering by keywords, categories, and series.
  - "Related Books" suggestions based on shared categories.

- **User Interaction & Social Features**
  - Add books to a personal Bookmark collection.
  - Track Reading Progress per book dynamically.
  - Rich nested Commenting system with replies, upvotes, and downvotes.
  - Book Reviews and dynamic 5-star rating aggregation.
  - Badges and achievements granted automatically based on user engagement.

- **Moderation & Auditing**
  - Report system for flagging inappropriate books or comments to moderators.
  - Real-time WebSocket notifications for approvals, replies, and system alerts.
  - Comprehensive Audit Logging for all significant administrative and user actions, backed by RabbitMQ.

## Tech Stack
- **Framework:** Spring Boot 3
- **Language:** Java 17
- **Database:** MySQL (Hibernate / Spring Data JPA)
- **Security:** Spring Security & JWT
- **Messaging:** RabbitMQ & WebSockets (STOMP)
- **Document Processing:** Apache PDFBox
- **Anti-virus:** ClamAV

## Getting Started

1. Set up a MySQL database and update your `application.properties` credentials.
2. Ensure a RabbitMQ instance is running on `localhost:5672`.
3. Ensure ClamAV is running on `localhost:3310` (or disable it in `application.yml`).
4. Run `./mvnw spring-boot:run` to start the application on port `8080`.
