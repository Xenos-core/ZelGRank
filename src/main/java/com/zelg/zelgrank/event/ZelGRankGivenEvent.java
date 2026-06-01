package com.zelg.zelgrank.event;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;

public class ZelGRankGivenEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID targetUuid;
    private final String targetName;
    private final String rankName;
    private final Duration duration;
    private final CommandSender actor;

    public ZelGRankGivenEvent(UUID targetUuid, String targetName, String rankName, @Nullable Duration duration, @Nullable CommandSender actor) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.rankName = rankName;
        this.duration = duration;
        this.actor = actor;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getRankName() {
        return rankName;
    }

    @Nullable
    public Duration getDuration() {
        return duration;
    }

    @Nullable
    public CommandSender getActor() {
        return actor;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
