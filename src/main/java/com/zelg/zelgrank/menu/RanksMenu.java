package com.zelg.zelgrank.menu;

import com.zelg.zelgrank.api.RankInfo;
import com.zelg.zelgrank.config.Messages;
import com.zelg.zelgrank.util.Durations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Read only view of someone's ranks, opened by /ginfo.
 * The holder is how {@link MenuListener} recognises it, instead of comparing titles.
 */
public final class RanksMenu implements InventoryHolder {

    public static final int CLOSE_SLOT = 49;
    private static final int HEAD_SLOT = 4;
    private static final int FIRST_RANK_SLOT = 9;
    private static final int LAST_RANK_SLOT = 44;

    private final Inventory inventory;

    public RanksMenu(Messages messages, String targetName, UUID targetUuid, List<RankInfo> ranks) {
        inventory = Bukkit.createInventory(this, 54, messages.get("menu.title", Messages.text("player", targetName)));

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, Component.empty(), List.of());
        for (int slot = 0; slot < 9; slot++) {
            inventory.setItem(slot, filler);
            inventory.setItem(45 + slot, filler);
        }

        inventory.setItem(HEAD_SLOT, head(messages, targetName, targetUuid, ranks.size()));

        int slot = FIRST_RANK_SLOT;
        for (RankInfo rank : ranks) {
            if (slot > LAST_RANK_SLOT) {
                break;
            }
            inventory.setItem(slot++, rankItem(messages, rank));
        }
        if (ranks.isEmpty()) {
            inventory.setItem(22, item(Material.PAPER, messages.get("menu.no-ranks"), List.of()));
        }

        inventory.setItem(CLOSE_SLOT, item(Material.BARRIER, messages.get("menu.close"), List.of()));
    }

    private static ItemStack head(Messages messages, String name, UUID uuid, int rankCount) {
        ItemStack head = item(Material.PLAYER_HEAD,
                messages.get("menu.head-name", Messages.text("player", name)),
                messages.list("menu.head-lore", Messages.text("count", rankCount)));
        head.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid)));
        return head;
    }

    private static ItemStack rankItem(Messages messages, RankInfo rank) {
        TagResolver placeholders = TagResolver.resolver(
                Messages.text("rank", rank.name()),
                Messages.text("weight", rank.weight()),
                Messages.text("time", rank.timeLeft().map(Durations::pretty).orElse("")));

        Material material = rank.isTemporary() ? Material.CLOCK : Material.NAME_TAG;
        String loreKey = rank.isTemporary() ? "menu.rank-lore-temporary" : "menu.rank-lore-permanent";
        return item(material, messages.get("menu.rank-name", placeholders), messages.list(loreKey, placeholders));
    }

    private static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            // menus render names in italics unless you tell them not to
            meta.displayName(name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            meta.lore(lore.stream()
                    .map(line -> line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                    .toList());
        });
        return item;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
