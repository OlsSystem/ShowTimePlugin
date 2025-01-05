package net.axolsystems.showtime.commands.cutscenes;

import net.axolsystems.showtime.CutsceneInfo;
import net.axolsystems.showtime.CutsceneManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AddCameraCommand implements CommandExecutor {
    private JavaPlugin plugin;
    private final CutsceneManager cutsceneManager;

    public AddCameraCommand(JavaPlugin plugin, CutsceneManager cutsceneManager) {
        this.plugin = plugin;
        this.cutsceneManager = cutsceneManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players may use this command");
            return false;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            sender.sendMessage("Usage /addcamera <cutscene_name> <duration_in_seconds>.");
            return false;
        }

        String name = args[0];
        int duration = Integer.parseInt(args[1]) * 20;

        CutsceneInfo cutsceneInfo = cutsceneManager.getCutscene(name);

        if (cutsceneInfo == null) {
            sender.sendMessage("Cutscene " + name + " cannot be found.");
            return false;
        }

        cutsceneInfo.addStep(player.getLocation(), duration);
        sender.sendMessage("Step has been added to the cutscene " + name + ".");
        return true;
    }
}
