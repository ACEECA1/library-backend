package org.personal.library.service.comment;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.BookRepository;
import org.personal.library.dao.CommentRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.dto.comment.CommentRequestDTO;
import org.personal.library.dto.comment.CommentResponseDTO;
import org.personal.library.model.Book;
import org.personal.library.model.Comment;
import org.personal.library.model.User;
import org.personal.library.util.AppException;
import org.personal.library.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addComment(Long bookId, CommentRequestDTO dto) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));

        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setBook(book);
        comment.setUser(user);
        comment.setDraft(dto.isDraft());

        if (dto.getParentCommentId() != null) {
            Comment parent = commentRepository.findById(dto.getParentCommentId())
                    .orElseThrow(() -> new AppException("Parent comment not found", HttpStatus.NOT_FOUND));
            comment.setParentComment(parent);
        }

        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getCommentsForBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));

        // In a real app we'd likely have a method findByBookIdAndParentCommentIsNull
        return commentRepository.findAll().stream()
                .filter(c -> c.getBook().getId().equals(bookId) && c.getParentComment() == null)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private CommentResponseDTO mapToDTO(Comment comment) {
        return CommentResponseDTO.builder()
                .id(comment.getId())
                .text(comment.getText())
                .upvotes(comment.getUpvotes())
                .downvotes(comment.getDownvotes())
                .username(comment.getUser().getUsername())
                .bookId(comment.getBook().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .createdAt(comment.getCreatedAt())
                .isDraft(comment.isDraft())
                .replies(comment.getReplies().stream().map(this::mapToDTO).collect(Collectors.toList()))
                .build();
    }
}
