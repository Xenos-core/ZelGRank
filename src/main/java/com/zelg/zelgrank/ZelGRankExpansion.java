package com.zelg.zelgrank;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class ZelGRankExpansion extends PlaceholderExpansion {
    private final ZelGRank plugin;
    private final LuckPerms luckPerms;

    public ZelGRankExpansion(ZelGRank plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "zelgrank";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Macronis";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return "";
        }

        switch (params.toLowerCase()) {
            case "rank":
                return getHighestRank(user);
            case "expires":
                return getExpiryTime(user);
            case "expires_formatted":
                return getFormattedExpiryTime(user);
            case "hasrank":
                return user.getNodes().stream()
                        .filter(node -> node instanceof InheritanceNode)
                        .findAny()
                        .isPresent() ? "true" : "false";
            case "rankcount":
                return String.valueOf(user.getNodes().stream()
                        .filter(node -> node instanceof InheritanceNode)
                        .count());
            default:
                if (params.toLowerCase().startsWith("hasrank_")) {
                    String rankName = params.substring(8);
                    return user.getNodes().stream()
                            .filter(node -> node instanceof InheritanceNode)
                            .map(node -> ((InheritanceNode) node).getGroupName())
                            .anyMatch(group -> group.equalsIgnoreCase(rankName)) ? "true" : "false";
                }
                if (params.toLowerCase().startsWith("expires_")) {
                    String rankName = params.substring(8);
                    return getSpecificRankExpiry(user, rankName);
                }
                return null;
        }
    }

    private String getHighestRank(User user) {
        return user.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode)
                .map(node -> ((InheritanceNode) node).getGroupName())
                .findFirst()
                .orElse("default");
    }

    private String getExpiryTime(User user) {
        return user.getNodes().stream()
                .filter(Node::hasExpiry)
                .filter(node -> node instanceof InheritanceNode)
                .map(Node::getExpiry)
                .filter(expiry -> expiry != null)
                .map(expiry -> {
                    Duration timeLeft = Duration.between(Instant.now(), expiry);
                    return timeLeft.toSeconds() > 0 ? String.valueOf(timeLeft.toSeconds()) : "0";
                })
                .findFirst()
                .orElse("Never");
    }

    private String getFormattedExpiryTime(User user) {
        return user.getNodes().stream()
                .filter(Node::hasExpiry)
                .filter(node -> node instanceof InheritanceNode)
                .map(Node::getExpiry)
                .filter(expiry -> expiry != null)
                .map(expiry -> {
                    Duration timeLeft = Duration.between(Instant.now(), expiry);
                    if (timeLeft.toSeconds() <= 0) return "Expired";
                    return formatDuration(timeLeft);
                })
                .findFirst()
                .orElse("Permanent");
    }

    private String getSpecificRankExpiry(User user, String rankName) {
        return user.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode)
                .filter(node -> ((InheritanceNode) node).getGroupName().equalsIgnoreCase(rankName))
                .filter(Node::hasExpiry)
                .map(Node::getExpiry)
                .filter(expiry -> expiry != null)
                .map(expiry -> {
                    Duration timeLeft = Duration.between(Instant.now(), expiry);
                    return timeLeft.toSeconds() > 0 ? formatDuration(timeLeft) : "Expired";
                })
                .findFirst()
                .orElse("Permanent");
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
}
