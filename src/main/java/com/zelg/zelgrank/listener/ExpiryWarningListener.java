package com.zelg.zelgrank.listener;

import com.zelg.zelgrank.ZelGRank;
import com.zelg.zelgrank.api.RankInfo;
import com.zelg.zelgrank.config.Messages;
import com.zelg.zelgrank.config.Settings;
import com.zelg.zelgrank.util.Durations;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/** Tells players on join when one of their temporary ranks is about to run out. */
public final class ExpiryWarningListener implements Listener {

    private final ZelGRank plugin;

    public ExpiryWarningListener(ZelGRank plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Settings settings = plugin.settings();
        if (!settings.expiryWarningEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        plugin.tasks().entityLater(player, () -> warn(player, settings.expiryWarningWithin()), settings.expiryWarningDelay());
    }

    private void warn(Player player, Duration within) {
        if (!player.isOnline()) {
            return;
        }

        // LuckPerms loads the user before they join, so the cached data is already there
        List<RankInfo> expiring = plugin.api().getCachedRanks(player.getUniqueId()).stream()
                .filter(rank -> rank.timeLeft().map(left -> !left.isNegative() && left.compareTo(within) <= 0).orElse(false))
                .sorted(Comparator.comparing(rank -> rank.expiry().orElseThrow()))
                .toList();
        if (expiring.isEmpty()) {
            return;
        }

        Messages messages = plugin.messages();
        messages.sendList(player, "expiry-warning.header");
        for (RankInfo rank : expiring) {
            messages.send(player, "expiry-warning.line",
                    Messages.text("rank", rank.name()),
                    Messages.text("time", Durations.pretty(rank.timeLeft().orElseThrow())));
        }
        messages.sendList(player, "expiry-warning.footer");
    }
}
