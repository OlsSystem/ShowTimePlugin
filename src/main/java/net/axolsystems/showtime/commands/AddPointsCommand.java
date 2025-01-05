package net.axolsystems.showtime.commands;

import net.axolsystems.showtime.ShowTime;
import net.axolsystems.showtime.ShowTimeAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

public class AddPointsCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public AddPointsCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("Usage: /addpoints <playername> <pointamount>");
            return false;
        }

        String playerName = args[0];
        int pointsToAdd = Integer.parseInt(args[1]);

        JSONObject payload = new JSONObject();
        payload.put("username", playerName);
        payload.put("pointAmount", pointsToAdd);
        payload.put("operation", "Add");

        ShowTime.logger.info("[DEBUG-POINTSYSTEM] Payload - " + payload.toString());

        boolean success = ShowTimeAPI.apiPostMethod("/api/pointsystem", payload);
        ShowTime.logger.info(String.valueOf(success));
        if (success) {
            sender.sendMessage(pointsToAdd + " points added to " + playerName + "!");
            ShowTime.updateTabList();
            for (Player player : Bukkit.getOnlinePlayers()) {
                ShowTime.updateScoreboard(player.getName());
            }
            return true;
        } else {
            sender.sendMessage("Failed to add " + pointsToAdd + " points to " + playerName + ".");
            return false;
        }
    }
}
