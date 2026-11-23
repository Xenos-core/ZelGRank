package com.zelg.zelgrank.command;

import com.zelg.zelgrank.ZelGRank;
import com.zelg.zelgrank.api.RankInfo;
import com.zelg.zelgrank.config.Messages;
import com.zelg.zelgrank.menu.RanksMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/** /ginfo &lt;player&gt;, opens a menu with the player's ranks and how long each one has left. */
public final class GInfoCommand implements TabExecutor {

    private final ZelGRank plugin;

    public GInfoCommand(ZelGRank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Messages messages = plugin.messages();
        if (!(sender instanceof Player viewer)) {
            messages.send(sender, "only-players");
            return true;
        }
        if (!viewer.hasPermission("zelgrank.info")) {
            messages.send(viewer, "no-permission");
            return true;
        }
        if (args.length < 1) {
            messages.send(viewer, "usage.ginfo");
            return true;
        }

        String targetName = args[0];
        plugin.api().lookupUniqueId(targetName)
                .thenCompose(uuid -> uuid
                        .map(id -> plugin.api().getRanks(id).thenApply(ranks -> new Target(id, ranks)))
                        .orElseGet(() -> CompletableFuture.completedFuture(null)))
                .whenComplete((target, error) -> plugin.tasks().entity(viewer, () -> {
                    if (error != null) {
                        plugin.getLogger().log(Level.WARNING, "Couldn't open /ginfo for " + targetName, error);
                        messages.send(viewer, "something-went-wrong");
                    } else if (target == null) {
                        messages.send(viewer, "player-not-found", Messages.text("player", targetName));
                    } else if (viewer.isOnline()) {
                        viewer.openInventory(new RanksMenu(messages, targetName, target.uuid(), target.ranks()).getInventory());
                    }
                }));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("zelgrank.info")) {
            return Suggest.players(args[0]);
        }
        return List.of();
    }

    private record Target(UUID uuid, List<RankInfo> ranks) {
    }
}
