package com.zelg.zelgrank.api;

import com.zelg.zelgrank.api.RankChangeResult.Status;
import com.zelg.zelgrank.event.ZelGRankGivenEvent;
import com.zelg.zelgrank.event.ZelGRankRemovedEvent;
import com.zelg.zelgrank.util.Tasks;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Public API, grab it from Bukkit's services manager:
 * <pre>{@code
 * ZelGRankApi api = Bukkit.getServicesManager().load(ZelGRankApi.class);
 * }</pre>
 * Every method that touches LuckPerms storage is async and returns a future. The
 * {@link ZelGRankGivenEvent} and {@link ZelGRankRemovedEvent} events are always fired
 * on the global region thread (the main thread on Paper).
 */
public class ZelGRankApi {

    private final LuckPerms luckPerms;
    private final Tasks tasks;

    public ZelGRankApi(LuckPerms luckPerms, Tasks tasks) {
        this.luckPerms = luckPerms;
        this.tasks = tasks;
    }

    /**
     * Gives a rank. If the player already has it for a limited time, the new duration is
     * added on top instead of being ignored. A permanent give replaces a temporary one.
     *
     * @param duration how long, or null for permanent
     * @param actor    who gave it, null when it came from code
     */
    public CompletableFuture<RankChangeResult> giveRank(UUID targetUuid, String targetName, String rankName,
                                                        @Nullable Duration duration, @Nullable CommandSender actor) {
        Group group = luckPerms.getGroupManager().getGroup(rankName);
        if (group == null) {
            return CompletableFuture.completedFuture(RankChangeResult.of(Status.UNKNOWN_RANK, rankName));
        }
        String rank = group.getName();

        return luckPerms.getUserManager().loadUser(targetUuid).thenCompose(user -> {
            if (user == null) {
                return CompletableFuture.completedFuture(RankChangeResult.of(Status.FAILED, rank));
            }

            List<InheritanceNode> owned = globalNodes(user, rank);
            if (owned.stream().anyMatch(node -> !node.hasExpiry())) {
                return CompletableFuture.completedFuture(RankChangeResult.of(Status.ALREADY_PERMANENT, rank));
            }
            boolean hadTemporary = !owned.isEmpty();

            RankChangeResult result;
            if (duration == null) {
                user.data().clear(sameGlobalGroup(rank));
                user.data().add(InheritanceNode.builder(rank).build());
                result = RankChangeResult.of(hadTemporary ? Status.MADE_PERMANENT : Status.GIVEN, rank);
            } else {
                InheritanceNode node = InheritanceNode.builder(rank).expiry(duration).build();
                DataMutateResult.WithMergedNode added = user.data().add(node, TemporaryNodeMergeStrategy.ADD_NEW_DURATION_TO_EXISTING);
                if (!added.getResult().wasSuccessful()) {
                    return CompletableFuture.completedFuture(RankChangeResult.of(Status.FAILED, rank));
                }
                result = RankChangeResult.of(hadTemporary ? Status.EXTENDED : Status.GIVEN, rank, added.getMergedNode().getExpiry());
            }

            return luckPerms.getUserManager().saveUser(user).thenApply(saved -> {
                tasks.global(() -> Bukkit.getPluginManager().callEvent(
                        new ZelGRankGivenEvent(targetUuid, targetName, rank, duration, actor, result)));
                return result;
            });
        });
    }

    /** Takes a rank away, temporary or not. */
    public CompletableFuture<RankChangeResult> removeRank(UUID targetUuid, String targetName, String rankName,
                                                          @Nullable CommandSender actor) {
        Group group = luckPerms.getGroupManager().getGroup(rankName);
        String rank = group != null ? group.getName() : rankName;

        return luckPerms.getUserManager().loadUser(targetUuid).thenCompose(user -> {
            if (user == null) {
                return CompletableFuture.completedFuture(RankChangeResult.of(Status.FAILED, rank));
            }
            if (globalNodes(user, rank).isEmpty()) {
                return CompletableFuture.completedFuture(RankChangeResult.of(Status.NOT_OWNED, rank));
            }

            user.data().clear(sameGlobalGroup(rank));
            return luckPerms.getUserManager().saveUser(user).thenApply(saved -> {
                tasks.global(() -> Bukkit.getPluginManager().callEvent(
                        new ZelGRankRemovedEvent(targetUuid, targetName, rank, actor)));
                return RankChangeResult.of(Status.REMOVED, rank);
            });
        });
    }

    /** All ranks of a player, online or not, highest weight first. */
    public CompletableFuture<List<RankInfo>> getRanks(UUID targetUuid) {
        return luckPerms.getUserManager().loadUser(targetUuid)
                .thenApply(user -> user == null ? List.of() : ranksOf(user));
    }

    /**
     * Same as {@link #getRanks(UUID)} but only for players LuckPerms already has in memory
     * (basically online players). Doesn't hit storage, so it's fine to call every tick.
     */
    public List<RankInfo> getCachedRanks(UUID targetUuid) {
        User user = luckPerms.getUserManager().getUser(targetUuid);
        return user == null ? List.of() : ranksOf(user);
    }

    public CompletableFuture<Boolean> hasRank(UUID targetUuid, String rankName) {
        return getRanks(targetUuid).thenApply(ranks -> ranks.stream().anyMatch(rank -> rank.name().equalsIgnoreCase(rankName)));
    }

    /** Online players are resolved right away, everyone else through LuckPerms' own name cache. */
    public CompletableFuture<Optional<UUID>> lookupUniqueId(String playerName) {
        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null) {
            return CompletableFuture.completedFuture(Optional.of(online.getUniqueId()));
        }
        return luckPerms.getUserManager().lookupUniqueId(playerName).thenApply(Optional::ofNullable);
    }

    public boolean rankExists(String rankName) {
        return luckPerms.getGroupManager().getGroup(rankName) != null;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    private List<RankInfo> ranksOf(User user) {
        return user.getNodes(NodeType.INHERITANCE).stream()
                .filter(node -> !node.hasExpired())
                .map(node -> new RankInfo(node.getGroupName(), weightOf(node.getGroupName()), Optional.ofNullable(node.getExpiry())))
                .distinct()
                .sorted(Comparator.comparingInt(RankInfo::weight).reversed().thenComparing(RankInfo::name))
                .toList();
    }

    private int weightOf(String groupName) {
        Group group = luckPerms.getGroupManager().getGroup(groupName);
        return group == null ? 0 : group.getWeight().orElse(0);
    }

    // ZelGRank only manages context-free nodes, ranks given with server=... stay LuckPerms' business
    private static List<InheritanceNode> globalNodes(User user, String rank) {
        return user.getNodes(NodeType.INHERITANCE).stream()
                .filter(node -> node.getContexts().isEmpty() && node.getGroupName().equalsIgnoreCase(rank))
                .toList();
    }

    private static Predicate<Node> sameGlobalGroup(String rank) {
        return NodeType.INHERITANCE.predicate(node -> node.getContexts().isEmpty() && node.getGroupName().equalsIgnoreCase(rank));
    }
}
