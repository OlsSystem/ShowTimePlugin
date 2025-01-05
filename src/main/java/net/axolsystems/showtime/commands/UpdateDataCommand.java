package net.axolsystems.showtime.commands;

import net.axolsystems.showtime.ShowTime;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class UpdateDataCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public UpdateDataCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ShowTime.updateTabList();
        for (Player player: Bukkit.getOnlinePlayers()) {
            ShowTime.updateScoreboard(player.getName());
        }

        ShowTime.logger.info("Data Updated.");
        sender.sendMessage("Scoreboard and tablist have been updated");
        return true;
    }


}
