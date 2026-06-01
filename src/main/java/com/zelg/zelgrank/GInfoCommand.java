package com.zelg.zelgrank;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class GInfoCommand implements CommandExecutor, Listener {
    private final ZelGRank plugin;
    private final LuckPerms luckPerms;
    private final MiniMessage miniMessage;

    public GInfoCommand(ZelGRank plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.miniMessage = MiniMessage.miniMessage();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.only-players", "<red>Only players can use this command!")));
            return true;
        }
        if (!player.hasPermission("zelgrank.info")) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.no-permission", "<red>You don't have permission to use this command!")));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.usage.ginfo", "<gold>Usage: /ginfo <player>")));
            return true;
        }
        String targetName = args[0];
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer != null) {
            openPermissionsGUI(player, targetPlayer.getName(), targetPlayer.getUniqueId());
        } else {
            luckPerms.getUserManager().lookupUniqueId(targetName).thenAccept(uuid -> {
                if (uuid == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(miniMessage.deserialize(plugin.getConfig().getString("messages.player-not-found", "<red>Player <yellow>{player}</yellow> has never joined the server!").replace("{player}", targetName))));
                } else {
                    openPermissionsGUI(player, targetName, uuid);
                }
            });
        }
        return true;
    }

    private void openPermissionsGUI(Player viewer, String targetName, UUID targetUUID) {
        luckPerms.getUserManager().loadUser(targetUUID).thenAcceptAsync(user -> {
            if (user == null) {
                Bukkit.getScheduler().runTask(plugin, () -> viewer.sendMessage(miniMessage.deserialize("<red>Failed to load user data!")));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                List<String> groups = new ArrayList<>();
                user.getNodes().forEach(node -> {
                    if (node.getKey().startsWith("group.")) {
                        groups.add(node.getKey().substring(6));
                    }
                });
                Inventory inv = Bukkit.createInventory(null, 54, Component.text("§a" + targetName + "'s Groups"));
                ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
                ItemMeta headMeta = playerHead.getItemMeta();
                headMeta.displayName(Component.text("§a§l" + targetName));
                headMeta.lore(Arrays.asList(
                    Component.text("§7Total Groups: §a" + groups.size())
                ));
                playerHead.setItemMeta(headMeta);
                inv.setItem(4, playerHead);
                int slot = 9;
                for (String group : groups) {
                    if (slot >= 45) break;
                    ItemStack item = new ItemStack(Material.NAME_TAG);
                    ItemMeta meta = item.getItemMeta();
                    meta.displayName(Component.text("§a" + group));
                    item.setItemMeta(meta);
                    inv.setItem(slot, item);
                    slot++;
                }
                ItemStack closeItem = new ItemStack(Material.BARRIER);
                ItemMeta closeMeta = closeItem.getItemMeta();
                closeMeta.displayName(Component.text("§cClose"));
                closeItem.setItemMeta(closeMeta);
                inv.setItem(49, closeItem);
                viewer.openInventory(inv);
            });
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().title().toString();
        if (!title.contains("'s Groups")) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }
}
