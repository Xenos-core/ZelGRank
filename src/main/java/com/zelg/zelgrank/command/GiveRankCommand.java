package com.zelg.zelgrank.command;

import com.zelg.zelgrank.ZelGRank;
import com.zelg.zelgrank.api.RankChangeResult;
import com.zelg.zelgrank.config.Messages;
import com.zelg.zelgrank.util.Durations;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * /giverank &lt;player&gt; &lt;rank&gt; [duration] [-s]
 * <p>
 * Giving a rank someone already has for a while adds the time on top.
 * -s skips the purchase broadcast, for when staff just hand out a rank.
 */
public final class GiveRankCommand implements TabExecutor {

    private final ZelGRank plugin;

    public GiveRankCommand(ZelGRank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] rawArgs) {
        Messages messages = plugin.messages();
        if (!sender.hasPermission("zelgrank.give")) {
            messages.send(sender, "no-permission");
            return true;
        }

        List<String> args = new ArrayList<>(Arrays.asList(rawArgs));
        boolean silent = args.removeIf(arg -> arg.equalsIgnoreCase("-s") || arg.equalsIgnoreCase("--silent"));
        if (args.size() < 2) {
            messages.send(sender, "usage.giverank");
            return true;
        }

        String targetName = args.get(0);
        String rankInput = args.get(1);
        if (!plugin.api().rankExists(rankInput)) {
            messages.send(sender, "rank-not-found", Messages.text("rank", rankInput));
            return true;
        }

        Duration duration = null;
        if (args.size() > 2) {
            duration = Durations.parse(args.get(2));
            if (duration == null) {
                messages.send(sender, "invalid-duration", Messages.text("input", args.get(2)));
                return true;
            }
        }

        Duration finalDuration = duration;
        plugin.api().lookupUniqueId(targetName)
                .thenCompose(uuid -> uuid
                        .map(id -> plugin.api().giveRank(id, targetName, rankInput, finalDuration, sender))
                        .orElseGet(() -> CompletableFuture.completedFuture(null)))
                .whenComplete((result, error) -> plugin.tasks().sender(sender, () -> {
                    if (error != null) {
                        plugin.getLogger().log(Level.WARNING, "Couldn't give " + rankInput + " to " + targetName, error);
                        messages.send(sender, "something-went-wrong");
                    } else if (result == null) {
                        messages.send(sender, "player-not-found", Messages.text("player", targetName));
                    } else {
                        reply(sender, targetName, finalDuration, silent, result);
                    }
                }));
        return true;
    }

    private void reply(CommandSender sender, String targetName, @Nullable Duration duration, boolean silent, RankChangeResult result) {
        Messages messages = plugin.messages();
        String rank = result.rank();
        String length = duration == null ? messages.plain("permanent") : Durations.toKey(duration);

        var player = Messages.text("player", targetName);
        var rankTag = Messages.text("rank", rank);
        var durationTag = Messages.text("duration", length);

        switch (result.status()) {
            case GIVEN -> messages.send(sender, "rank-given", player, rankTag, durationTag);
            case EXTENDED -> {
                String left = result.expiry()
                        .map(expiry -> Durations.pretty(Duration.between(Instant.now(), expiry)))
                        .orElse(length);
                messages.send(sender, "rank-extended", player, rankTag, durationTag, Messages.text("time", left));
            }
            case MADE_PERMANENT -> messages.send(sender, "rank-made-permanent", player, rankTag);
            case ALREADY_PERMANENT -> messages.send(sender, "rank-already-permanent", player, rankTag);
            case UNKNOWN_RANK -> messages.send(sender, "rank-not-found", rankTag);
            default -> messages.send(sender, "something-went-wrong");
        }

        if (result.success() && !silent) {
            plugin.broadcast().announce(targetName, rank, duration);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zelgrank.give")) {
            return List.of();
        }
        return switch (args.length) {
            case 1 -> Suggest.players(args[0]);
            case 2 -> Suggest.groups(plugin.api().getLuckPerms(), args[1]);
            case 3 -> Suggest.filter(withSilent(Suggest.DURATIONS), args[2]);
            case 4 -> Suggest.filter(List.of("-s"), args[3]);
            default -> List.of();
        };
    }

    private static List<String> withSilent(List<String> options) {
        List<String> all = new ArrayList<>(options);
        all.add("-s");
        return all;
    }
}
