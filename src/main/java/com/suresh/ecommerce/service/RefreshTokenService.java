package com.suresh.ecommerce.service;

import com.suresh.ecommerce.entity.RefreshToken;
import com.suresh.ecommerce.entity.User;
import com.suresh.ecommerce.exception.TokenRefreshException;
import com.suresh.ecommerce.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    // One active refresh token per user: replaces any existing one on a fresh login,
    // rather than accumulating a new row every time someone logs in.
    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(),
                    "Refresh token was expired. Please sign in again.");
        }
        return token;
    }

    // Used on logout — deletes the user's refresh token so it can no longer be
    // exchanged for a new JWT. The short-lived access token already in the client's
    // hands still works until it naturally expires (JWTs can't be revoked without a
    // server-side blocklist), but the refresh chain is cut, so the session can't renew.
    // Identifying the user via their authenticated principal (not a client-supplied
    // token) means logout can't be used to revoke someone else's session.
    // @Transactional is required here: deleteByUser is a @Modifying bulk-delete query,
    // and Spring Data JPA refuses to run a modifying query without an active transaction
    // ("No active transaction for update or delete query") — unlike save()/findById(),
    // which open one implicitly, @Query-based updates/deletes need it explicit.
    @Transactional
    public void revokeByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}