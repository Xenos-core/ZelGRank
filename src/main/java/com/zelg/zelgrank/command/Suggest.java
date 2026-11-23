package com.zelg.zelgrank.command;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Tab completion helpers shared by the commands. */
final class Suggest {

    static final List<String> DURATIONS = List.of("1h", "12h", "1d", "3d", "7d", "14d", "30d", "1M", "3M", "1y");

    private Suggest() {
    }

    static List<String> players(String typed) {
        return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), typed);
    }

    static List<String> groups(LuckPerms luckPerms, String typed) {
        return filter(luckPerms.getGroupManager().getLoadedGroups().stream()
                .map(Group::getName)
                .sorted()
                .toList(), typed);
    }

    static List<String> filter(Collection<String> options, String typed) {
        String lower = typed.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }
}
