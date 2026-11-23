package com.zelg.zelgrank;

import com.zelg.zelgrank.api.ZelGRankApi;
import com.zelg.zelgrank.broadcast.PurchaseBroadcast;
import com.zelg.zelgrank.command.DemoteCommand;
import com.zelg.zelgrank.command.GInfoCommand;
import com.zelg.zelgrank.command.GiveRankCommand;
import com.zelg.zelgrank.command.ZelGRankCommand;
import com.zelg.zelgrank.config.Messages;
import com.zelg.zelgrank.config.Settings;
import com.zelg.zelgrank.hook.ZelGRankExpansion;
import com.zelg.zelgrank.listener.ExpiryWarningListener;
import com.zelg.zelgrank.menu.MenuListener;
import com.zelg.zelgrank.util.Tasks;
import net.luckperms.api.LuckPerms;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZelGRank extends JavaPlugin {

    private final Messages messages = new Messages(this);
    private Settings settings;
    private Tasks tasks;
    private ZelGRankApi api;
    private PurchaseBroadcast broadcast;

    @Override
    public void onEnable() {
        LuckPerms luckPerms = getServer().getServicesManager().load(LuckPerms.class);
        if (luckPerms == null) {
            getLogger().severe("LuckPerms isn't loaded, ZelGRank can't do anything without it.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        reload();

        tasks = new Tasks(this);
        api = new ZelGRankApi(luckPerms, tasks);
        broadcast = new PurchaseBroadcast(this);
        getServer().getServicesManager().register(ZelGRankApi.class, api, this, ServicePriority.Normal);

        register("giverank", new GiveRankCommand(this));
        register("demote", new DemoteCommand(this));
        register("ginfo", new GInfoCommand(this));
        register("zelgrank", new ZelGRankCommand(this));

        getServer().getPluginManager().registerEvents(new ExpiryWarningListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ZelGRankExpansion(this).register();
            getLogger().info("Hooked into PlaceholderAPI");
        }
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (tasks != null) {
            tasks.cancelAll();
        }
    }

    /** Re-reads config.yml and messages.yml. */
    public void reload() {
        reloadConfig();
        settings = new Settings(getConfig(), getLogger());
        messages.reload();
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("/" + name + " is missing from plugin.yml");
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public Messages messages() {
        return messages;
    }

    public Settings settings() {
        return settings;
    }

    public Tasks tasks() {
        return tasks;
    }

    public ZelGRankApi api() {
        return api;
    }

    public PurchaseBroadcast broadcast() {
        return broadcast;
    }
}
