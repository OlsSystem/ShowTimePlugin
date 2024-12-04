package net.axolsystems.showtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ShowTimeUpdate {

    private static ShowTimeUpdate instance;
    private static String gitHubRepo = "https://api.github.com/repos/OlsSystem/ShowTimePlugin/releases";


    public static ShowTimeUpdate getInstance() {
        if (instance == null) {
            instance = new ShowTimeUpdate();
        }
        return instance;
    }

    public void autoUpdate() {
        pluginUpdating(null, () -> {
            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[ASCORE] A new version of the plugin has been downloaded!");
        });
    }

    public void pluginUpdating(CommandSender sender, Runnable onComplete) {
        try {
            String version = ShowTime.version;
            String parseVersion = version.replace(".", "");

            URL apiUrl = new URL(gitHubRepo + "/latest");
            URLConnection con = apiUrl.openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);

            JsonObject json = JsonParser.parseReader(new InputStreamReader(con.getInputStream())).getAsJsonObject();
            String tagName = json.get("tag_name").getAsString();
            String parsedTagName = tagName.replace(".", "");

            int latestVersion = Integer.parseInt(parsedTagName.substring(1));

            URL download = new URL(gitHubRepo + "/downoad/" + tagName + "/ShowTime.jar");

            if (latestVersion > Integer.parseInt(parseVersion)) {
                String message = ChatColor.GREEN + "[ASCORE] New Version found: " + ChatColor.RED + "ShowTime Release" + tagName + ChatColor.LIGHT_PURPLE + " downloading new version now..";
                Bukkit.getConsoleSender().sendMessage(message);
                if (sender != null) {
                    sender.sendMessage(message);
                } 
                ShowTime.logger.info(message);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try (InputStream in = download.openStream()) {
                            File temp = new File("plugins/showtimeUpdates");
                            if (!temp.exists()) {
                                temp.mkdirs();
                            }

                            Path path = new File(temp, "ShowTime.jar").toPath();
                            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);

                            if (onComplete != null) {
                                Bukkit.getScheduler().runTask(ShowTime.getInstance(), onComplete);
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.runTaskLaterAsynchronously(ShowTime.getInstance(), 0);
            } else {
                String message = ChatColor.GREEN + "[ASCORE] No New Version for Show Time Found: " + ChatColor.RED + tagName + ChatColor.LIGHT_PURPLE + " is the current up to date version of ShowTime.";
                Bukkit.getConsoleSender().sendMessage(message);
                if (sender != null) {
                    sender.sendMessage(message);
                } 
                ShowTime.logger.info(message);
            }

        } catch (IOException e) {
            e.printStackTrace();
            String message = ChatColor.RED + "[ASCORE] An error occurred while checking for updates.";
            Bukkit.getConsoleSender().sendMessage(message);
            if (sender != null) {
                sender.sendMessage(message);
            }
            ShowTime.logger.warning(message);
        }

    }
    
}
