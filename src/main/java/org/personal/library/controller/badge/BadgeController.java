package org.personal.library.controller.badge;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.UserBadgeRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.model.User;
import org.personal.library.model.UserBadge;
import org.personal.library.util.AppException;
import org.personal.library.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<String>>> getMyBadges() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new AppException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        List<String> badges = userBadgeRepository.findByUserId(user.getId())
                .stream()
                .map(UserBadge::getBadgeType)
                .map(Enum::name)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(badges));
    }
}
