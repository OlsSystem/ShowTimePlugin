package net.axolsystems.showtime.commands.cutscenes;

import net.axolsystems.showtime.CutsceneManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayCutsceneCommand implements CommandExecutor {
    private JavaPlugin plugin;
    private final CutsceneManager cutsceneManager;

    public PlayCutsceneCommand(JavaPlugin plugin, CutsceneManager cutsceneManager) {
        this.plugin = plugin;
        this.cutsceneManager = cutsceneManager;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 2) {
            sender.sendMessage("Usage: /playcutscene <player> <cutscene_name>");
            return false;
        }

        String playerName = args[1];
        String cutsceneName = args[2];

        Player player = Bukkit.getPlayer(playerName);

        cutsceneManager.playCutscene(player, cutsceneName);
        return true;
    }
}
