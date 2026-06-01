package com.zelg.zelgrank;

import com.zelg.zelgrank.api.RankChangeResult;
import com.zelg.zelgrank.api.ZelGRankApi;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class DemoteCommand implements CommandExecutor {
    private final ZelGRank plugin;
    private final ZelGRankApi api;
    private final MiniMessage miniMessage;

    public DemoteCommand(ZelGRank plugin, ZelGRankApi api) {
        this.plugin = plugin;
        this.api = api;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zelgrank.demote")) {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.no-permission", "<red>You don't have permission to use this command!")));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.usage.demote", "<gold>Usage: /demote <player> <rank>")));
            return true;
        }

        String targetName = args[0];
        String rankName = args[1];
        if (!api.rankExists(rankName)) {
            sender.sendMessage(miniMessage.deserialize("<red>Rank <yellow>" + rankName + "</yellow> does not exist in LuckPerms!"));
            return true;
        }

        api.lookupUniqueId(targetName).thenAccept(optionalUuid -> {
            if (optionalUuid.isEmpty()) {
                sendSync(sender, plugin.getConfig().getString("messages.player-not-found", "<red>Player <yellow>{player}</yellow> has never joined the server!").replace("{player}", targetName));
                return;
            }

            api.removeRank(optionalUuid.get(), targetName, rankName, sender)
                    .thenAccept(result -> handleRemoveResult(result, targetName, rankName, sender));
        }).exceptionally(throwable -> {
            plugin.getLogger().warning("Failed to remove rank: " + throwable.getMessage());
            sendSync(sender, "<red>Failed to remove rank. Check console for details.");
            return null;
        });
        return true;
    }

    private void handleRemoveResult(RankChangeResult result, String targetName, String rankName, CommandSender sender) {
        if (!result.success()) {
            sendSync(sender, "<red>" + result.message());
            return;
        }

        sendSync(sender, plugin.getConfig().getString("messages.rank-removed", "<green>Successfully removed rank <yellow>{rank}</yellow> from <yellow>{player}</yellow>!").replace("{player}", targetName).replace("{rank}", rankName));
    }

    private void sendSync(CommandSender sender, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(miniMessage.deserialize(message)));
    }
}
