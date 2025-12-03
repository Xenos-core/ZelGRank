package com.zelg.zelgrank;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

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
        luckPerms.getUserManager().lookupUniqueId(targetName).thenAcceptAsync(uuid -> {
            if (uuid == null) {
                sender.sendMessage(miniMessage.deserialize("<red>Player <yellow>" + targetName + "</yellow> has never joined the server!"));
                return;
            }
            demoteRank(uuid, targetName, rankName, sender);
        });
        return true;
    }

    private void demoteRank(UUID targetUUID, String targetName, String rankName, CommandSender sender) {
        luckPerms.getUserManager().loadUser(targetUUID).thenAcceptAsync(user -> {
            if (user == null) {
                sender.sendMessage(miniMessage.deserialize("<red>Failed to load user data!"));
                return;
            }
            user.data().clear(node -> node instanceof InheritanceNode && ((InheritanceNode) node).getGroupName().equalsIgnoreCase(rankName));
            luckPerms.getUserManager().saveUser(user).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.rank-removed", "<green>Successfully removed rank <yellow>{rank}</yellow> from <yellow>{player}</yellow>!").replace("{player}", targetName).replace("{rank}", rankName)));
                });
            });
        });
    }
}
