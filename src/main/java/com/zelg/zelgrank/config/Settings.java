package com.zelg.zelgrank.config;

import com.zelg.zelgrank.util.Durations;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * config.yml read once into plain fields, so nothing has to go through getConfig() at runtime.
 */
public final class Settings {

    public enum Recipients { ALL, STAFF, NONE }

    private final boolean broadcastEnabled;
    private final Recipients recipients;
    private final String staffPermission;
    private final List<String> broadcastLines;
    private final @Nullable Sound broadcastSound;
    private final @Nullable ConfigurationSection prices;
    private final boolean expiryWarningEnabled;
    private final Duration expiryWarningWithin;
    private final long expiryWarningDelay;

    public Settings(FileConfiguration config, Logger logger) {
        broadcastEnabled = config.getBoolean("broadcast.enabled", true);
        recipients = parseRecipients(config.getString("broadcast.recipients", "all"), logger);
        staffPermission = config.getString("broadcast.staff-permission", "zelgrank.notify");
        broadcastLines = config.getStringList("broadcast.lines");
        broadcastSound = parseSound(config, logger);
        prices = config.getConfigurationSection("rank-prices");

        expiryWarningEnabled = config.getBoolean("expiry-warning.enabled", true);
        Duration within = Durations.parse(config.getString("expiry-warning.within", "24h"));
        if (within == null) {
            logger.warning("expiry-warning.within isn't a valid duration, using 24h");
            within = Duration.ofHours(24);
        }
        expiryWarningWithin = within;
        expiryWarningDelay = Math.max(1, config.getLong("expiry-warning.delay-ticks", 40));
    }

    /**
     * Price for the broadcast. Looks for a per-duration price first (rank-prices.vip.30d),
     * then the rank's own default, then the global default.
     */
    public String priceFor(String rank, @Nullable Duration duration) {
        if (prices == null) {
            return "0.00";
        }
        String fallback = prices.getString("default", "0.00");

        String key = findKey(prices, rank);
        if (key == null) {
            return fallback;
        }
        if (!prices.isConfigurationSection(key)) {
            return prices.getString(key, fallback);
        }

        ConfigurationSection perDuration = prices.getConfigurationSection(key);
        String fallbackForRank = perDuration.getString("default", fallback);
        if (duration == null) {
            return perDuration.getString("permanent", fallbackForRank);
        }
        // compare as durations so 7d, 1w and 168h all hit the same price
        for (String entry : perDuration.getKeys(false)) {
            if (duration.equals(Durations.parse(entry))) {
                return perDuration.getString(entry, fallbackForRank);
            }
        }
        return fallbackForRank;
    }

    public boolean broadcastEnabled() {
        return broadcastEnabled && recipients != Recipients.NONE;
    }

    public Recipients recipients() {
        return recipients;
    }

    public String staffPermission() {
        return staffPermission;
    }

    public List<String> broadcastLines() {
        return broadcastLines;
    }

    public @Nullable Sound broadcastSound() {
        return broadcastSound;
    }

    public boolean expiryWarningEnabled() {
        return expiryWarningEnabled;
    }

    public Duration expiryWarningWithin() {
        return expiryWarningWithin;
    }

    public long expiryWarningDelay() {
        return expiryWarningDelay;
    }

    // yaml keys are case sensitive, LuckPerms group names aren't
    private static @Nullable String findKey(ConfigurationSection section, String wanted) {
        if (section.contains(wanted)) {
            return wanted;
        }
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase(wanted)) {
                return key;
            }
        }
        return null;
    }

    private static Recipients parseRecipients(String value, Logger logger) {
        try {
            return Recipients.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger.warning("broadcast.recipients must be all, staff or none, got '" + value + "'. Using all.");
            return Recipients.ALL;
        }
    }

    private static @Nullable Sound parseSound(FileConfiguration config, Logger logger) {
        if (!config.getBoolean("broadcast.sound.enabled", true)) {
            return null;
        }
        String name = config.getString("broadcast.sound.name", "entity.experience_orb.pickup");
        try {
            return Sound.sound(Key.key(name), Sound.Source.MASTER,
                    (float) config.getDouble("broadcast.sound.volume", 1.0),
                    (float) config.getDouble("broadcast.sound.pitch", 1.0));
        } catch (InvalidKeyException e) {
            // most likely the old ENTITY_EXPERIENCE_ORB_PICKUP style from 1.x configs
            logger.warning("broadcast.sound.name '" + name + "' isn't a sound key (like entity.experience_orb.pickup), the broadcast will be silent");
            return null;
        }
    }
}
