package com.zelg.zelgrank.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Everything players and staff read lives in messages.yml.
 * <p>
 * Keys missing from the server's copy fall back to the one bundled in the jar, so old
 * files keep working after an update. Setting a message to "" turns it off.
 */
public final class Messages {

    // older configs used {player} style placeholders, quietly turn them into <player> tags
    private static final Pattern LEGACY_PLACEHOLDER = Pattern.compile("\\{([a-z_]+)}");

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration config;
    private TagResolver prefix = TagResolver.empty();

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource("messages.yml")) {
            if (in != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Couldn't read the bundled messages.yml: " + e.getMessage());
        }

        prefix = Placeholder.parsed("prefix", config.getString("prefix", ""));
    }

    public void send(CommandSender to, String key, TagResolver... placeholders) {
        String raw = config.getString(key);
        if (raw == null || raw.isEmpty()) {
            return;
        }
        to.sendMessage(parse(raw, placeholders));
    }

    public void sendList(CommandSender to, String key, TagResolver... placeholders) {
        for (Component line : list(key, placeholders)) {
            to.sendMessage(line);
        }
    }

    public Component get(String key, TagResolver... placeholders) {
        return parse(config.getString(key, key), placeholders);
    }

    /** A message with its formatting stripped, for when it ends up inside another placeholder. */
    public String plain(String key) {
        return PlainTextComponentSerializer.plainText().serialize(get(key));
    }

    public List<Component> list(String key, TagResolver... placeholders) {
        List<Component> lines = new ArrayList<>();
        for (String raw : config.getStringList(key)) {
            lines.add(parse(raw, placeholders));
        }
        return lines;
    }

    /** Parses any MiniMessage string with the same placeholders, used for config.yml lines too. */
    public Component parse(String raw, TagResolver... placeholders) {
        String text = LEGACY_PLACEHOLDER.matcher(raw).replaceAll("<$1>");
        return miniMessage.deserialize(text, TagResolver.resolver(prefix, TagResolver.resolver(placeholders)));
    }

    /** Placeholder for user supplied text like names, so a rank called "<red>" can't inject formatting. */
    public static TagResolver text(String key, Object value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }
}
