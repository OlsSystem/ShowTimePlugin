package net.axolsystems.showtime.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;

public class HelpCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public HelpCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("----- ShowTime Commands -----");
        Map<String, Map<String, Object>> commands = plugin.getCommand("help").getPlugin().getDescription().getCommands();

        sender.sendMessage("  --- Contestant Commands ---  ");
        for (String cmd : commands.keySet()) {
            String permission = (String) commands.get(cmd).get("permission");
            if (permission == null || sender.hasPermission(permission)) {
                String description = (String) commands.get(cmd).get("description");
                if (permission.equals("showtime.contestant")) {
                    sender.sendMessage("/" + cmd + " - " + description);
                }
            }
        }

        sender.sendMessage("\n  --- Organisers Commands --- ");
        for (String cmd : commands.keySet()) {
            String permission = (String) commands.get(cmd).get("permission");
            if (permission == null || sender.hasPermission(permission)) {
                String description = (String) commands.get(cmd).get("description");
                if (permission.equals("showtime.admin")) {
                    sender.sendMessage("/" + cmd + " - " + description);
                }
            }
        }

        sender.sendMessage("-----------------------");
        return true;
    }
}
