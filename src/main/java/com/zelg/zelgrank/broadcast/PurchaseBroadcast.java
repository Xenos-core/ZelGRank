package com.zelg.zelgrank.broadcast;

import com.zelg.zelgrank.ZelGRank;
import com.zelg.zelgrank.config.Messages;
import com.zelg.zelgrank.config.Settings;
import com.zelg.zelgrank.util.Durations;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The "RANK PURCHASE" announcement. Only used by /giverank, never by the API,
 * so other plugins giving ranks don't spam chat.
 */
public final class PurchaseBroadcast {

    private final ZelGRank plugin;

    public PurchaseBroadcast(ZelGRank plugin) {
        this.plugin = plugin;
    }

    public void announce(String playerName, String rank, @Nullable Duration duration) {
        Settings settings = plugin.settings();
        if (!settings.broadcastEnabled() || settings.broadcastLines().isEmpty()) {
            return;
        }

        Messages messages = plugin.messages();
        String length = duration == null ? messages.plain("permanent") : Durations.toKey(duration);
        TagResolver placeholders = TagResolver.resolver(
                Messages.text("player", playerName),
                Messages.text("rank", rank),
                Messages.text("duration", length),
                Messages.text("price", settings.priceFor(rank, duration)));

        List<Component> lines = new ArrayList<>();
        for (String line : settings.broadcastLines()) {
            lines.add(messages.parse(line, placeholders));
        }

        Sound sound = settings.broadcastSound();
        boolean staffOnly = settings.recipients() == Settings.Recipients.STAFF;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (staffOnly && !player.hasPermission(settings.staffPermission())) {
                continue;
            }
            plugin.tasks().entity(player, () -> {
                lines.forEach(player::sendMessage);
                if (sound != null) {
                    player.playSound(sound, Sound.Emitter.self());
                }
            });
        }
        plugin.getLogger().info(playerName + " got " + rank + " (" + length + ")");
    }
}
