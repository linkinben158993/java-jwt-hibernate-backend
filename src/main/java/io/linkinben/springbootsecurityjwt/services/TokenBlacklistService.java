package io.linkinben.springbootsecurityjwt.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// TODO: Replace the in-memory map with a DB-backed table (e.g. `blacklisted_tokens`):
//   columns: token_hash VARCHAR(64) PK, expires_at DATETIME, created_at DATETIME
//   Store SHA-256(rawJwt) instead of the raw string to keep rows compact.
//   The cleanup @Scheduled below can then run: DELETE FROM blacklisted_tokens WHERE expires_at < NOW()
//   This survives restarts and works across multiple instances.
@Slf4j
@Service
public class TokenBlacklistService {

    // rawJwt → expiration timestamp (ms since epoch).
    // Cleanup criterion: if expiresAt < now, the token would already be rejected by JWTUtils as expired,
    // so the blacklist entry is no longer needed and can be removed safely.
    private final Map<String, Long> blacklist = Collections.synchronizedMap(new HashMap<>());

    public void add(String rawJwt, long expiresAtMs) {
        blacklist.put(rawJwt, expiresAtMs);
    }

    public boolean isBlacklisted(String rawJwt) {
        return blacklist.containsKey(rawJwt);
    }

    // Runs every 5 minutes. Removes entries whose natural expiry has already passed —
    // those tokens would be rejected by the JWT parser anyway, so the blacklist entry is redundant.
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(e -> e.getValue() < now);
        int removed = before - blacklist.size();
        if (removed > 0) {
            log.info("Token blacklist cleanup: removed {} expired entries, {} remaining", removed, blacklist.size());
        }
    }
}
