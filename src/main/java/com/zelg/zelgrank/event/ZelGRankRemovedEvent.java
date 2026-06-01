package com.zelg.zelgrank.event;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ZelGRankRemovedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID targetUuid;
    private final String targetName;
    private final String rankName;
    private final CommandSender actor;

    public ZelGRankRemovedEvent(UUID targetUuid, String targetName, String rankName, @Nullable CommandSender actor) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.rankName = rankName;
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
