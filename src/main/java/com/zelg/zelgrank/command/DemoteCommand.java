package com.zelg.zelgrank.command;

import com.zelg.zelgrank.ZelGRank;
import com.zelg.zelgrank.api.RankInfo;
import com.zelg.zelgrank.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/** /demote &lt;player&gt; &lt;rank&gt; */
public final class DemoteCommand implements TabExecutor {

    private final ZelGRank plugin;

    public DemoteCommand(ZelGRank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Messages messages = plugin.messages();
        if (!sender.hasPermission("zelgrank.demote")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage.demote");
            return true;
        }

        String targetName = args[0];
        String rankInput = args[1];
        plugin.api().lookupUniqueId(targetName)
                .thenCompose(uuid -> uuid
                        .map(id -> plugin.api().removeRank(id, targetName, rankInput, sender))
                        .orElseGet(() -> CompletableFuture.completedFuture(null)))
                .whenComplete((result, error) -> plugin.tasks().sender(sender, () -> {
                    if (error != null) {
                        plugin.getLogger().log(Level.WARNING, "Couldn't remove " + rankInput + " from " + targetName, error);
                        messages.send(sender, "something-went-wrong");
                        return;
                    }
                    if (result == null) {
                        messages.send(sender, "player-not-found", Messages.text("player", targetName));
                        return;
                    }

                    var player = Messages.text("player", targetName);
                    var rank = Messages.text("rank", result.rank());
                    switch (result.status()) {
                        case REMOVED -> messages.send(sender, "rank-removed", player, rank);
                        case NOT_OWNED -> messages.send(sender, "rank-not-owned", player, rank);
                        default -> messages.send(sender, "something-went-wrong");
                    }
                }));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zelgrank.demote")) {
            return List.of();
        }
        if (args.length == 1) {
            return Suggest.players(args[0]);
        }
        if (args.length == 2) {
            // for online players we know their ranks, so only suggest those
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target != null) {
                List<String> owned = plugin.api().getCachedRanks(target.getUniqueId()).stream().map(RankInfo::name).toList();
                return Suggest.filter(owned, args[1]);
            }
            return Suggest.groups(plugin.api().getLuckPerms(), args[1]);
        }
        return List.of();
    }
}
