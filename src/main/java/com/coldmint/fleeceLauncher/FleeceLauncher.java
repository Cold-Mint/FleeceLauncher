package com.coldmint.fleeceLauncher;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class FleeceLauncher extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        LauncherListener launcherListener = new LauncherListener();
        Bukkit.getPluginManager().registerEvents(launcherListener, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
