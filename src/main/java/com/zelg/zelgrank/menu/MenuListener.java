package com.zelg.zelgrank.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class MenuListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof RanksMenu)) {
            return;
        }
        // nothing in there is meant to be taken, including shift clicks from the player's own inventory
        event.setCancelled(true);

        if (event.getClickedInventory() == event.getView().getTopInventory()
                && event.getRawSlot() == RanksMenu.CLOSE_SLOT
                && event.getWhoClicked() instanceof Player player) {
            player.closeInventory();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof RanksMenu) {
            event.setCancelled(true);
        }
    }
}
