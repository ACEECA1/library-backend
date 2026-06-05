package org.personal.library.service.security;

import lombok.RequiredArgsConstructor;
import org.personal.library.config.AppProperties;
import org.personal.library.dao.RefreshTokenRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.model.RefreshToken;
import org.personal.library.model.User;
import org.personal.library.util.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final AppProperties appProperties;

    /**
     * Create refresh token.
     *
     * @param userId the userId
     * @return the refreshtoken
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user).orElse(new RefreshToken());
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(appProperties.getJwt().getRefreshTokenExpirationMs()));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verify expiration.
     *
     * @param token the token
     * @return the refreshtoken
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new AppException("Refresh token was expired. Please make a new signin request", HttpStatus.UNAUTHORIZED);
        }
        return token;
    }

    /**
     * Delete by user id.
     *
     * @param userId the userId
     */
    @Transactional
    public void deleteByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        refreshTokenRepository.deleteByUser(user);
    }
    
    /**
     * Find by token.
     *
     * @param token the token
     * @return the optional
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
}
