package com.zelg.zelgrank;

import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class ZelGRank extends JavaPlugin {
    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!setupLuckPerms()) {
            getLogger().severe("LuckPerms not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("giverank").setExecutor(new GiveRankCommand(this, luckPerms));
        getCommand("demote").setExecutor(new DemoteCommand(this, luckPerms));
        getCommand("ginfo").setExecutor(new GInfoCommand(this, luckPerms));
        getCommand("zelgrank").setExecutor(new ReloadCommand(this));
        getServer().getPluginManager().registerEvents(new RankExpiryNotifier(this, luckPerms), this);
        getLogger().info("ZelGRank has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ZelGRank has been disabled!");
    }

    private boolean setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
            return true;
        }
        return false;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
