package com.stufy.fragmc.redempt;

import com.stufy.fragmc.redempt.commands.PromoCommand;
import com.stufy.fragmc.redempt.commands.RedeemCommand;
import com.stufy.fragmc.redempt.database.DatabaseManager;
import com.stufy.fragmc.redempt.managers.PromoCodeManager;
import net.ess3.api.IEssentials;
import org.bukkit.plugin.java.JavaPlugin;

public class Redempt extends JavaPlugin {

    private DatabaseManager databaseManager;
    private PromoCodeManager promoCodeManager;
    private IEssentials essentials;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize EssentialsX
        essentials = (IEssentials) getServer().getPluginManager().getPlugin("Essentials");
        if (essentials == null) {
            getLogger().severe("EssentialsX not found! This plugin requires EssentialsX to function.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize database
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // Initialize managers
        promoCodeManager = new PromoCodeManager(this, databaseManager);

        // Register commands
        getCommand("promo").setExecutor(new PromoCommand(this, promoCodeManager));
        getCommand("redeem").setExecutor(new RedeemCommand(this, promoCodeManager));

        getLogger().info("Redempt has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("Redempt has been disabled!");
    }

    public IEssentials getEssentials() {
        return essentials;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PromoCodeManager getPromoCodeManager() {
        return promoCodeManager;
    }
}