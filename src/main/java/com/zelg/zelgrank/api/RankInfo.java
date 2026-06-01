package com.zelg.zelgrank.api;

import java.time.Instant;
import java.util.Optional;

public record RankInfo(String name, Optional<Instant> expiry) {
    public boolean isTemporary() {
        return expiry.isPresent();
    }
}
