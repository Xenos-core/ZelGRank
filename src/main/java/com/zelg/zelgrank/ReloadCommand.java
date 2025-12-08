package com.zelg.zelgrank;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {
    private final ZelGRank plugin;
    private final MiniMessage miniMessage;

    public ReloadCommand(ZelGRank plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zelgrank.reload")) {
            sender.sendMessage(miniMessage.deserialize("<red>You don't have permission to use this command!"));
            return true;
        }
        try {
            plugin.reloadConfig();
            sender.sendMessage(miniMessage.deserialize("<green>ZelGRank configuration reloaded successfully!"));
        } catch (Exception e) {
            sender.sendMessage(miniMessage.deserialize("<red>Failed to reload configuration: " + e.getMessage()));
            plugin.getLogger().severe("Error reloading config: " + e.getMessage());
        }
        return true;
    }
}
