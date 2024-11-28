package net.axolsystems;

import org.bukkit.plugin.java.JavaPlugin;

public class ShowTime extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Systems Online.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Systems Off.");
    }
}