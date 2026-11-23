package com.zelg.zelgrank.api;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;

/**
 * What actually happened when ZelGRank tried to change someone's rank.
 *
 * @param status what happened
 * @param rank   the group name as LuckPerms stores it
 * @param expiry when the rank now runs out, empty for permanent ranks and failures
 */
public record RankChangeResult(Status status, String rank, Optional<Instant> expiry) {

    public enum Status {
        /** The player didn't have the rank and got it. */
        GIVEN(true),
        /** The player already had it temporarily and the new time was added on top. */
        EXTENDED(true),
        /** The player had it temporarily and it was swapped for a permanent one. */
        MADE_PERMANENT(true),
        /** The rank was taken away. */
        REMOVED(true),
        /** The player already has the rank permanently, nothing to give. */
        ALREADY_PERMANENT(false),
        /** Tried to remove a rank the player doesn't have. */
        NOT_OWNED(false),
        /** No LuckPerms group with that name. */
        UNKNOWN_RANK(false),
        /** LuckPerms couldn't load or save the user. */
        FAILED(false);

        private final boolean success;

        Status(boolean success) {
            this.success = success;
        }

        public boolean success() {
            return success;
        }
    }

    public static RankChangeResult of(Status status, String rank) {
        return new RankChangeResult(status, rank, Optional.empty());
    }

    public static RankChangeResult of(Status status, String rank, @Nullable Instant expiry) {
        return new RankChangeResult(status, rank, Optional.ofNullable(expiry));
    }

    public boolean success() {
        return status.success();
    }
}
