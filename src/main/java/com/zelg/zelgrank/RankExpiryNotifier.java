package com.zelg.zelgrank;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RankExpiryNotifier implements Listener {
    private final ZelGRank plugin;
    private final LuckPerms luckPerms;
    private final MiniMessage miniMessage;

    public RankExpiryNotifier(ZelGRank plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        luckPerms.getUserManager().loadUser(player.getUniqueId()).thenAcceptAsync(user -> {
            if (user == null) return;
            List<String> expiringRanks = new ArrayList<>();
            for (Node node : user.getNodes()) {
                if (node instanceof InheritanceNode inheritanceNode) {
                    if (node.hasExpiry()) {
                        Instant expiry = node.getExpiry();
                        if (expiry != null) {
                            Duration timeLeft = Duration.between(Instant.now(), expiry);
                            if (timeLeft.toHours() <= 24 && timeLeft.toSeconds() > 0) {
                                String rankName = inheritanceNode.getGroupName();
                                String timeString = formatDuration(timeLeft);
                                expiringRanks.add(rankName + ":" + timeString);
                            }
                        }
                    }
                }
            }
            if (!expiringRanks.isEmpty()) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.rank-expiry-header", "<gold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
                    player.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.rank-expiry-title", "<red><bold>⚠ RANK EXPIRATION WARNING ⚠</bold>")));
                    player.sendMessage(miniMessage.deserialize(""));
                    for (String rankInfo : expiringRanks) {
                        String[] parts = rankInfo.split(":");
                        String rankName = parts[0];
                        String timeLeft = parts[1];
                        player.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.rank-expiry-line", "<yellow>• Rank <gold><bold>{rank}</bold></gold> expires in: <red>{time}</red>").replace("{rank}", rankName).replace("{time}", timeLeft)));
                    }
                    player.sendMessage(miniMessage.deserialize(""));
                    player.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.rank-expiry-renew", "<green>Renew your rank at: <white>@NoxelStore")));
                    player.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.rank-expiry-footer", "<gold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
                });
            }
        });
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
}
