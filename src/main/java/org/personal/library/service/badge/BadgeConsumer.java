package org.personal.library.service.badge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.personal.library.config.RabbitMQConfig;
import org.personal.library.dao.BookRepository;
import org.personal.library.dao.CommentRepository;
import org.personal.library.dao.ReviewRepository;
import org.personal.library.dao.UserBadgeRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.dto.badge.BadgeMessage;
import org.personal.library.model.BadgeType;
import org.personal.library.model.User;
import org.personal.library.model.UserBadge;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeConsumer {

    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final CommentRepository commentRepository;

    /**
     * Processes badge evaluation messages received from the RabbitMQ queue.
     * Evaluates whether the user qualifies for new badges based on their upload, review, or upvote activity.
     *
     * @param message the badge message containing user ID and action type
     */
    @RabbitListener(queues = RabbitMQConfig.BADGE_QUEUE)
    @Transactional
    public void processBadge(BadgeMessage message) {
        log.info("Processing badge evaluation for User ID: {}, Action: {}", message.getUserId(), message.getActionType());
        
        try {
            User user = userRepository.findById(message.getUserId()).orElse(null);
            if (user == null) return;

            switch (message.getActionType()) {
                case "UPLOAD":
                    evaluateUploadBadges(user);
                    break;
                case "REVIEW":
                    evaluateReviewBadges(user);
                    break;
                case "UPVOTE":
                    evaluateUpvoteBadges(user);
                    break;
            }
        } catch (Exception e) {
            log.error("Error evaluating badges", e);
        }
    }

    private void evaluateUploadBadges(User user) {
        long uploadCount = bookRepository.countByUploaderId(user.getId());
        awardBadgeIfApplicable(user, BadgeType.UPLOADER_BRONZE, uploadCount >= 1);
        awardBadgeIfApplicable(user, BadgeType.UPLOADER_SILVER, uploadCount >= 10);
        awardBadgeIfApplicable(user, BadgeType.UPLOADER_GOLD, uploadCount >= 50);
    }

    private void evaluateReviewBadges(User user) {
        long reviewCount = reviewRepository.countByUserId(user.getId());
        awardBadgeIfApplicable(user, BadgeType.REVIEWER_BRONZE, reviewCount >= 1);
        awardBadgeIfApplicable(user, BadgeType.REVIEWER_SILVER, reviewCount >= 10);
        awardBadgeIfApplicable(user, BadgeType.REVIEWER_GOLD, reviewCount >= 50);
    }

    private void evaluateUpvoteBadges(User user) {
        
        
        
        boolean hasPopularComment = commentRepository.existsByUserIdAndUpvotesGreaterThanEqual(user.getId(), 10);
        awardBadgeIfApplicable(user, BadgeType.POPULAR_COMMENTER, hasPopularComment);
    }

    private void awardBadgeIfApplicable(User user, BadgeType type, boolean conditionMet) {
        if (conditionMet && !userBadgeRepository.existsByUserIdAndBadgeType(user.getId(), type)) {
            UserBadge badge = new UserBadge(user, type);
            userBadgeRepository.save(badge);
            log.info("Awarded badge {} to user {}", type, user.getUsername());
        }
    }
}
