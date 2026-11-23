package com.zelg.zelgrank.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * One rank a player has.
 *
 * @param name   group name
 * @param weight LuckPerms group weight, 0 when the group has none
 * @param expiry when it runs out, empty for permanent ranks
 */
public record RankInfo(String name, int weight, Optional<Instant> expiry) {

    public boolean isTemporary() {
        return expiry.isPresent();
    }

    /** Time left, or empty for permanent ranks. */
    public Optional<Duration> timeLeft() {
        return expiry.map(at -> Duration.between(Instant.now(), at));
    }
}
