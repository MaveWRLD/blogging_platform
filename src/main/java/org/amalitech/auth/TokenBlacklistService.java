package org.amalitech.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String token, Instant expiry) {
        blacklist.put(token, expiry);
    }

    public boolean isBlacklisted(String token) {
        Instant expiry = blacklist.get(token);

        if (expiry == null) return false;

        if (expiry.isBefore(Instant.now())) {
            blacklist.remove(token);
            return false;
        }

        return true;
    }

    @Scheduled(fixedRate = 60_000)
    public void cleanup() {
        Instant now = Instant.now();
        blacklist.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}
