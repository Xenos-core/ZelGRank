package com.zelg.zelgrank;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DemoteCommand implements CommandExecutor {
    private final ZelGRank plugin;
    private final LuckPerms luckPerms;
    private final MiniMessage miniMessage;

    public DemoteCommand(ZelGRank plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zelgrank.demote")) {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.no-permission", "<red>You don't have permission to use this command!")));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(miniMessage.deserialize("<gold>Usage: /demote <player> <rank>"));
            return true;
        }
        String targetName = args[0];
        String rankName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer == null) {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.player-not-found", "<red>Player not found!").replace("{player}", targetName)));
            return true;
        }
        demoteRank(targetPlayer, rankName, sender);
        return true;
    }

    private void demoteRank(Player targetPlayer, String rankName, CommandSender sender) {
        luckPerms.getUserManager().loadUser(targetPlayer.getUniqueId()).thenAcceptAsync(user -> {
            if (user == null) {
                sender.sendMessage(miniMessage.deserialize("<red>Failed to load user data!"));
                return;
            }
            InheritanceNode node = InheritanceNode.builder(rankName).build();
            user.data().remove(node);
            luckPerms.getUserManager().saveUser(user).thenRun(() -> {
                sender.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.rank-removed", "<green>Successfully removed rank <yellow>{rank}</yellow> from <yellow>{player}</yellow>!").replace("{player}", targetPlayer.getName()).replace("{rank}", rankName)));
            });
        });
    }
}
