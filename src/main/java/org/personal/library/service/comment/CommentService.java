package org.personal.library.service.comment;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.BookRepository;
import org.personal.library.dao.CommentRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.dto.comment.CommentRequestDTO;
import org.personal.library.dto.comment.CommentResponseDTO;
import org.personal.library.dto.common.PaginatedResponse;
import org.personal.library.model.Book;
import org.personal.library.model.Comment;
import org.personal.library.model.CommentVote;
import org.personal.library.model.User;
import org.personal.library.dao.CommentVoteRepository;
import org.personal.library.util.AppException;
import org.personal.library.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CommentVoteRepository commentVoteRepository;
    private final org.personal.library.service.audit.AuditLogService auditLogService;
    private final org.personal.library.service.notification.NotificationService notificationService;

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
        
        if (!comment.isDraft()) {
            auditLogService.logAction("ADD_COMMENT", "Added comment to book ID: " + bookId);
            if (!user.getId().equals(book.getUploader().getId()) && book.getUploader() != null) {
                notificationService.createForUser(book.getUploader(), "Someone commented on your book: " + book.getTitle());
            }
        }
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<CommentResponseDTO> getCommentsForBook(Long bookId, Pageable pageable) {
        Page<CommentResponseDTO> page = commentRepository.findByBookIdAndParentCommentIsNullAndIsDraftFalse(bookId, pageable)
                .map(this::mapToDTO);
        return PaginatedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public java.util.List<CommentResponseDTO> getUserDraftsForBook(Long bookId) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        return commentRepository.findByBookIdAndUserIdAndIsDraftTrue(bookId, user.getId())
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public void updateComment(Long commentId, CommentRequestDTO dto) {
        String username = SecurityUtils.getCurrentUsername();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException("Comment not found", HttpStatus.NOT_FOUND));
        if (!comment.getUser().getUsername().equals(username)) {
            throw new AppException("You do not have permission to update this comment", HttpStatus.FORBIDDEN);
        }
        comment.setText(dto.getText());
        boolean wasDraft = comment.isDraft();
        comment.setDraft(dto.isDraft());
        commentRepository.save(comment);

        if (wasDraft && !comment.isDraft()) {
            auditLogService.logAction("PUBLISH_COMMENT", "Published draft comment ID: " + commentId);
            Book book = comment.getBook();
            if (!comment.getUser().getId().equals(book.getUploader().getId()) && book.getUploader() != null) {
                notificationService.createForUser(book.getUploader(), "Someone commented on your book: " + book.getTitle());
            }
        }
    }

    @Transactional
    public void upvoteComment(Long commentId) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) throw new AppException("Unauthorized", HttpStatus.UNAUTHORIZED);
        User user = userRepository.findByUsername(username).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException("Comment not found", HttpStatus.NOT_FOUND));

        CommentVote vote = commentVoteRepository.findByUserIdAndCommentId(user.getId(), comment.getId()).orElse(null);
        if (vote != null) {
            if (vote.getVoteType() == CommentVote.VoteType.UP) return; // already upvoted
            // changing from DOWN to UP
            vote.setVoteType(CommentVote.VoteType.UP);
            comment.setDownvotes(comment.getDownvotes() - 1);
            comment.setUpvotes(comment.getUpvotes() + 1);
        } else {
            vote = new CommentVote(user, comment, CommentVote.VoteType.UP);
            comment.setUpvotes(comment.getUpvotes() + 1);
        }
        commentVoteRepository.save(vote);
        commentRepository.save(comment);
    }

    @Transactional
    public void downvoteComment(Long commentId) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) throw new AppException("Unauthorized", HttpStatus.UNAUTHORIZED);
        User user = userRepository.findByUsername(username).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException("Comment not found", HttpStatus.NOT_FOUND));

        CommentVote vote = commentVoteRepository.findByUserIdAndCommentId(user.getId(), comment.getId()).orElse(null);
        if (vote != null) {
            if (vote.getVoteType() == CommentVote.VoteType.DOWN) return; // already downvoted
            // changing from UP to DOWN
            vote.setVoteType(CommentVote.VoteType.DOWN);
            comment.setUpvotes(comment.getUpvotes() - 1);
            comment.setDownvotes(comment.getDownvotes() + 1);
        } else {
            vote = new CommentVote(user, comment, CommentVote.VoteType.DOWN);
            comment.setDownvotes(comment.getDownvotes() + 1);
        }
        commentVoteRepository.save(vote);
        commentRepository.save(comment);
    }

    private CommentResponseDTO mapToDTO(Comment comment) {
        return CommentResponseDTO.builder()
                .id(comment.getId())
                .text(comment.getText())
                .username(comment.getUser().getUsername())
                .createdAt(comment.getCreatedAt())
                .upvotes(comment.getUpvotes())
                .downvotes(comment.getDownvotes())
                .bookId(comment.getBook().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .isDraft(comment.isDraft())
                .replies(comment.getReplies().stream().filter(c -> !c.isDraft()).map(this::mapToDTO).collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException("Comment not found", HttpStatus.NOT_FOUND));

        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(username).orElseThrow();
        boolean isModerator = user.getRoles().stream()
                .anyMatch(r -> r.getPermissions().stream()
                        .anyMatch(p -> p.getName() == org.personal.library.model.PermissionType.MODERATE_COMMENTS));

        if (!comment.getUser().getUsername().equals(username) && !isModerator) {
            throw new AppException("You do not have permission to delete this comment", HttpStatus.FORBIDDEN);
        }

        commentRepository.delete(comment);
    }
}
