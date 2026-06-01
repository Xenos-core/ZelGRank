package com.zelg.zelgrank;

import com.zelg.zelgrank.api.ZelGRankApi;
import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class ZelGRank extends JavaPlugin {
    private LuckPerms luckPerms;
    private ZelGRankApi api;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!setupLuckPerms()) {
            getLogger().severe("LuckPerms not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        api = new ZelGRankApi(this, luckPerms);
        getServer().getServicesManager().register(ZelGRankApi.class, api, this, ServicePriority.Normal);

        getCommand("giverank").setExecutor(new GiveRankCommand(this, api));
        getCommand("demote").setExecutor(new DemoteCommand(this, api));
        getCommand("ginfo").setExecutor(new GInfoCommand(this, luckPerms));
        getCommand("zelgrank").setExecutor(new ReloadCommand(this));
        getServer().getPluginManager().registerEvents(new RankExpiryNotifier(this, luckPerms), this);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ZelGRankExpansion(this, luckPerms).register();
            getLogger().info("PlaceholderAPI integration enabled!");
        }
        getLogger().info("ZelGRank has been enabled!");
    }

    @Override
    public void onDisable() {
        if (api != null) {
            getServer().getServicesManager().unregister(ZelGRankApi.class, api);
        }
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

    public ZelGRankApi getApi() {
        return api;
    }
}
