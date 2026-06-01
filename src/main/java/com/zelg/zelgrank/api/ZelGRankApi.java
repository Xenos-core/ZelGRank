package com.zelg.zelgrank.api;

import com.zelg.zelgrank.ZelGRank;
import com.zelg.zelgrank.event.ZelGRankGivenEvent;
import com.zelg.zelgrank.event.ZelGRankRemovedEvent;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ZelGRankApi {
    private final ZelGRank plugin;
    private final LuckPerms luckPerms;

    public ZelGRankApi(ZelGRank plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
    }

    public CompletableFuture<RankChangeResult> giveRank(UUID targetUuid, String targetName, String rankName, @Nullable Duration duration, @Nullable CommandSender actor) {
        return luckPerms.getUserManager().loadUser(targetUuid).thenCompose(user -> {
            if (user == null) {
                return CompletableFuture.completedFuture(RankChangeResult.failure("Failed to load user data"));
            }

            InheritanceNode.Builder nodeBuilder = InheritanceNode.builder(rankName);
            if (duration != null) {
                nodeBuilder.expiry(duration);
            }

            user.data().add(nodeBuilder.build());
            return luckPerms.getUserManager().saveUser(user).thenApply(ignored -> {
                runSync(() -> Bukkit.getPluginManager().callEvent(new ZelGRankGivenEvent(targetUuid, targetName, rankName, duration, actor)));
                return RankChangeResult.ok();
            });
        });
    }

    public CompletableFuture<RankChangeResult> removeRank(UUID targetUuid, String targetName, String rankName, @Nullable CommandSender actor) {
        return luckPerms.getUserManager().loadUser(targetUuid).thenCompose(user -> {
            if (user == null) {
                return CompletableFuture.completedFuture(RankChangeResult.failure("Failed to load user data"));
            }

            user.data().clear(node -> node instanceof InheritanceNode inheritanceNode && inheritanceNode.getGroupName().equalsIgnoreCase(rankName));
            return luckPerms.getUserManager().saveUser(user).thenApply(ignored -> {
                runSync(() -> Bukkit.getPluginManager().callEvent(new ZelGRankRemovedEvent(targetUuid, targetName, rankName, actor)));
                return RankChangeResult.ok();
            });
        });
    }

    public CompletableFuture<List<RankInfo>> getRanks(UUID targetUuid) {
        return luckPerms.getUserManager().loadUser(targetUuid).thenApply(user -> {
            if (user == null) {
                return List.of();
            }

            return user.getNodes().stream()
                    .filter(node -> node instanceof InheritanceNode)
                    .map(node -> {
                        InheritanceNode inheritanceNode = (InheritanceNode) node;
                        return new RankInfo(inheritanceNode.getGroupName(), Optional.ofNullable(node.getExpiry()));
                    })
                    .toList();
        });
    }

    public CompletableFuture<Boolean> hasRank(UUID targetUuid, String rankName) {
        return getRanks(targetUuid).thenApply(ranks -> ranks.stream().anyMatch(rank -> rank.name().equalsIgnoreCase(rankName)));
    }

    public CompletableFuture<Optional<UUID>> lookupUniqueId(String playerName) {
        return luckPerms.getUserManager().lookupUniqueId(playerName).thenApply(Optional::ofNullable);
    }

    public boolean rankExists(String rankName) {
        return luckPerms.getGroupManager().getGroup(rankName) != null;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    private void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }
}
