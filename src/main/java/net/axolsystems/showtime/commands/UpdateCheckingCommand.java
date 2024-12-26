package net.axolsystems.showtime.commands;

import net.axolsystems.showtime.ShowTimeUpdate;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class UpdateCheckingCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public UpdateCheckingCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("showtime.admin.updator")) {
            sender.sendMessage(ChatColor.GREEN + "Checking for new Updates...");
            ShowTimeUpdate.getInstance().pluginUpdating(sender, () -> {
                sender.sendMessage(ChatColor.GREEN + "New Plugin Version has been downloaded.");
            });
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "You do not have the correct permissions to run this command");
            return false;
        }
    }
}
