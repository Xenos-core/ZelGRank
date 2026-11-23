package com.zelg.zelgrank.hook;

import com.zelg.zelgrank.ZelGRank;
import com.zelg.zelgrank.api.RankInfo;
import com.zelg.zelgrank.util.Durations;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * PlaceholderAPI placeholders. All of them read LuckPerms' cache, so they only have
 * values for online players (that's what scoreboards and tab lists need anyway).
 *
 * <ul>
 *   <li>%zelgrank_rank% highest weight rank, "default" when they have none</li>
 *   <li>%zelgrank_rankcount% how many ranks they have</li>
 *   <li>%zelgrank_hasrank% true if they have any rank</li>
 *   <li>%zelgrank_hasrank_&lt;rank&gt;% true or false</li>
 *   <li>%zelgrank_expires% seconds until their next temporary rank ends</li>
 *   <li>%zelgrank_expires_formatted% same, as 3d 4h</li>
 *   <li>%zelgrank_expires_&lt;rank&gt;% time left on that rank, or Permanent</li>
 * </ul>
 */
public final class ZelGRankExpansion extends PlaceholderExpansion {

    private final ZelGRank plugin;

    public ZelGRankExpansion(ZelGRank plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "zelgrank";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // sorted by weight, highest first. Everyone has "default", so it doesn't count as a rank here
        List<RankInfo> ranks = plugin.api().getCachedRanks(player.getUniqueId()).stream()
                .filter(rank -> !rank.name().equalsIgnoreCase("default"))
                .toList();
        String param = params.toLowerCase(Locale.ROOT);

        switch (param) {
            case "rank":
                return ranks.isEmpty() ? "default" : ranks.getFirst().name();
            case "rankcount":
                return String.valueOf(ranks.size());
            case "hasrank":
                return String.valueOf(!ranks.isEmpty());
            case "expires":
                return nextExpiry(ranks).map(left -> String.valueOf(left.getSeconds())).orElse("Never");
            case "expires_formatted":
                return nextExpiry(ranks).map(Durations::pretty).orElse("Permanent");
            default:
                break;
        }

        if (param.startsWith("hasrank_")) {
            String wanted = param.substring("hasrank_".length());
            return String.valueOf(find(ranks, wanted).isPresent());
        }
        if (param.startsWith("expires_")) {
            String wanted = param.substring("expires_".length());
            return find(ranks, wanted)
                    .map(rank -> rank.timeLeft().map(Durations::pretty).orElse("Permanent"))
                    .orElse("");
        }
        return null;
    }

    private static Optional<RankInfo> find(List<RankInfo> ranks, String name) {
        return ranks.stream().filter(rank -> rank.name().equalsIgnoreCase(name)).findFirst();
    }

    private static Optional<Duration> nextExpiry(List<RankInfo> ranks) {
        return ranks.stream()
                .map(RankInfo::timeLeft)
                .flatMap(Optional::stream)
                .filter(left -> !left.isNegative())
                .min(Comparator.naturalOrder());
    }
}
