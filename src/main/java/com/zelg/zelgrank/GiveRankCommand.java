package com.zelg.zelgrank;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GiveRankCommand implements CommandExecutor {
    private final ZelGRank plugin;
    private final LuckPerms luckPerms;
    private final MiniMessage miniMessage;
    private final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdwMy])");

    public GiveRankCommand(ZelGRank plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zelgrank.give")) {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.no-permission", "<red>You don't have permission to use this command!")));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(miniMessage.deserialize("<gold>Usage: /giverank <player> <rank> [duration]"));
            sender.sendMessage(miniMessage.deserialize("<gray>Duration format: <yellow>30s</yellow>, <yellow>5m</yellow>, <yellow>1h</yellow>, <yellow>7d</yellow>, <yellow>1w</yellow>, <yellow>1M</yellow>, <yellow>1y"));
            return true;
        }
        String targetName = args[0];
        String rankName = args[1];
        String durationString = args.length > 2 ? args[2] : null;
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer == null) {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.player-not-found", "<red>Player not found!").replace("{player}", targetName)));
            return true;
        }
        Duration duration = null;
        if (durationString != null) {
            try {
                duration = parseDuration(durationString);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(miniMessage.deserialize("<red>Invalid duration format! Use: 30s, 5m, 1h, 7d, 1w, 1M, 1y"));
                return true;
            }
        }
        giveRank(targetPlayer, rankName, duration, sender);
        return true;
    }

    private void giveRank(Player targetPlayer, String rankName, Duration duration, CommandSender sender) {
        luckPerms.getUserManager().loadUser(targetPlayer.getUniqueId()).thenAcceptAsync(user -> {
            if (user == null) {
                sender.sendMessage(miniMessage.deserialize("<red>Failed to load user data!"));
                return;
            }
            InheritanceNode.Builder nodeBuilder = InheritanceNode.builder(rankName);
            if (duration != null) {
                nodeBuilder.expiry(duration);
            }
            InheritanceNode node = nodeBuilder.build();
            user.data().add(node);
            luckPerms.getUserManager().saveUser(user).thenRun(() -> {
                String durationText = duration != null ? formatDuration(duration) : "permanent";
                sender.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.rank-given", "<green>Successfully gave rank <yellow>{rank}</yellow> to <yellow>{player}</yellow> for <yellow>{duration}</yellow>!").replace("{player}", targetPlayer.getName()).replace("{rank}", rankName).replace("{duration}", durationText)));
                String price = plugin.getConfig().getString("rank-prices." + rankName, plugin.getConfig().getString("rank-prices.default", "0.00"));
                List<String> broadcastLines = plugin.getConfig().getStringList("messages.broadcast");
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(Component.text(""));
                    onlinePlayer.sendMessage(miniMessage.deserialize("<green>" + getPlayerHead() + " <bold>" + targetPlayer.getName() + "</bold>"));
                    for (String line : broadcastLines) {
                        Component lineComponent = miniMessage.deserialize(line.replace("{player}", targetPlayer.getName()).replace("{rank}", rankName).replace("{price}", price));
                        onlinePlayer.sendMessage(lineComponent);
                    }
                    if (plugin.getConfig().getBoolean("sound.enabled", true)) {
                        try {
                            String soundName = plugin.getConfig().getString("sound.type", "ENTITY_EXPERIENCE_ORB_PICKUP");
                            Sound sound = Sound.valueOf(soundName);
                            float volume = (float) plugin.getConfig().getDouble("sound.volume", 1.0);
                            float pitch = (float) plugin.getConfig().getDouble("sound.pitch", 1.0);
                            onlinePlayer.playSound(onlinePlayer.getLocation(), sound, volume, pitch);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid sound type in config: " + e.getMessage());
                        }
                    }
                }
            });
        });
    }

    private Duration parseDuration(String input) throws IllegalArgumentException {
        Matcher matcher = TIME_PATTERN.matcher(input);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid duration format");
        }
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        return switch (unit) {
            case "s" -> Duration.of(amount, ChronoUnit.SECONDS);
            case "m" -> Duration.of(amount, ChronoUnit.MINUTES);
            case "h" -> Duration.of(amount, ChronoUnit.HOURS);
            case "d" -> Duration.of(amount, ChronoUnit.DAYS);
            case "w" -> Duration.of(amount * 7, ChronoUnit.DAYS);
            case "M" -> Duration.of(amount * 30, ChronoUnit.DAYS);
            case "y" -> Duration.of(amount * 365, ChronoUnit.DAYS);
            default -> throw new IllegalArgumentException("Invalid time unit");
        };
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "h";
        } else if (seconds < 604800) {
            return (seconds / 86400) + "d";
        } else if (seconds < 2592000) {
            return (seconds / 604800) + "w";
        } else if (seconds < 31536000) {
            return (seconds / 2592000) + "M";
        } else {
            return (seconds / 31536000) + "y";
        }
    }

    private String getPlayerHead() {
        return "☻";
    }
}
