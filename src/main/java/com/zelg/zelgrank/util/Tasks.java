package com.zelg.zelgrank.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Small wrapper around Paper's region aware schedulers.
 * <p>
 * On Folia there is no single main thread, so anything touching a player has to run on
 * that player's region and everything else on the global region. On regular Paper both
 * schedulers just run on the main thread, so the same code works on both.
 */
public final class Tasks {

    private final Plugin plugin;

    public Tasks(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Runs on the global region (the main thread on Paper). */
    public void global(Runnable task) {
        if (Bukkit.isGlobalTickThread()) {
            task.run();
            return;
        }
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    /** Runs on the thread that owns this entity. Skipped if the entity is gone (player logged out). */
    public void entity(Entity entity, Runnable task) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            task.run();
            return;
        }
        entity.getScheduler().run(plugin, scheduled -> task.run(), null);
    }

    public void entityLater(Entity entity, Runnable task, long delayTicks) {
        entity.getScheduler().runDelayed(plugin, scheduled -> task.run(), null, Math.max(1, delayTicks));
    }

    /** Players get their own region, console and command blocks go to the global one. */
    public void sender(CommandSender sender, Runnable task) {
        if (sender instanceof Entity entity) {
            entity(entity, task);
        } else {
            global(task);
        }
    }

    public void cancelAll() {
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
    }
}
