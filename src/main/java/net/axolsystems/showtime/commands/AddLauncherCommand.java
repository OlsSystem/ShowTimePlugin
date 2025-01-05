package net.axolsystems.showtime.commands;

import net.axolsystems.showtime.ShowTime;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AddLauncherCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public AddLauncherCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Location targetBlockLocation = player.getTargetBlockExact(5).getLocation();
            if (targetBlockLocation != null && targetBlockLocation.getBlock().getType() == Material.SLIME_BLOCK) {
                ShowTime.launcherBlocks.add(targetBlockLocation);
                player.sendMessage("This slime block is now a launcher.");
                return true;
            } else {
                player.sendMessage("The block your looking at must be a slime block.");
                return false;
            }
        } else {
            sender.sendMessage("This command can only be used by players");
            return false;
        }
    }
}
