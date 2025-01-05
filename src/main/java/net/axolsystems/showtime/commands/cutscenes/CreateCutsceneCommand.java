package net.axolsystems.showtime.commands.cutscenes;

import net.axolsystems.showtime.CutsceneManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class CreateCutsceneCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final CutsceneManager cutsceneManager;

    public CreateCutsceneCommand(JavaPlugin plugin, CutsceneManager cutsceneManager) {
        this.plugin = plugin;
        this.cutsceneManager = cutsceneManager;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /createcutscene <name>");
            return false;
        }

        String name = args[0];
        cutsceneManager.createCutscene(name);
        sender.sendMessage("Created the cutscene: " + name);
        return true;
    }
}
