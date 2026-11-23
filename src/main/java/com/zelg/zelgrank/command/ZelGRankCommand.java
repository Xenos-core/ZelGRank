package com.zelg.zelgrank.command;

import com.zelg.zelgrank.ZelGRank;
import com.zelg.zelgrank.config.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Level;

/** /zelgrank reload */
public final class ZelGRankCommand implements TabExecutor {

    private final ZelGRank plugin;

    public ZelGRankCommand(ZelGRank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Messages messages = plugin.messages();
        if (!sender.hasPermission("zelgrank.reload")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            messages.send(sender, "usage.zelgrank");
            return true;
        }

        try {
            plugin.reload();
            messages.send(sender, "reload-success");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Reload failed", e);
            messages.send(sender, "reload-failure", Messages.text("error", String.valueOf(e.getMessage())));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("zelgrank.reload")) {
            return Suggest.filter(List.of("reload"), args[0]);
        }
        return List.of();
    }
}
