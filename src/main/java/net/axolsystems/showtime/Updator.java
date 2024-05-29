package net.axolsystems.showtime;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Updator {

    private static Updator instance;

    public static Updator getInstance() {
        if (instance == null) {
            instance = new Updator();
        }
        return instance;
    }

    public void autoUpdate() {
        pluginUpdating(null, () -> {
            // This code block will be executed when the update is completed
            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Plugin update downloaded successfully!");
            // Add any other actions you want to perform after the update here
        });
    }

    public void pluginUpdating(CommandSender sender, Runnable onComplete) {
        try {
            String version = ShowTime.version;
            String parseVersion = version.replace(".", "");

            URL api = new URL("https://api.github.com/repos/OlsSystem/ShowTimePlugin/releases/latest");
            URLConnection con = api.openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);

            JsonObject json = JsonParser.parseReader(new InputStreamReader(con.getInputStream())).getAsJsonObject();
            String tagname = json.get("tag_name").getAsString();
            String parsedTagName = tagname.replace(".", "");

            int latestVersion = Integer.parseInt(parsedTagName.substring(1));

            URL download = new URL("https://github.com/OlsSystem/ShowTimePlugin/releases/download/" + tagname + "/ShowTime.jar");

            if (latestVersion > Integer.parseInt(parseVersion)) {
                String message = ChatColor.GREEN + "[SHOWTIME] Found a new version " + ChatColor.RED + tagname + ChatColor.LIGHT_PURPLE + " downloading now!!";
                Bukkit.getConsoleSender().sendMessage(message);
                if (sender != null) {
                    sender.sendMessage(message);
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try (InputStream in = download.openStream()) {
                            File temp = new File("plugins/update");
                            if (!temp.exists()) {
                                temp.mkdirs();
                            }
                            Path path = new File(temp, "ShowTime.jar").toPath();
                            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);

                            // Notify onComplete callback when download is completed
                            if (onComplete != null) {
                                Bukkit.getScheduler().runTask(ShowTime.getInstance(), onComplete);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.runTaskLaterAsynchronously(ShowTime.getInstance(), 0); // Run asynchronously
            } else {
                String message = ChatColor.GREEN + "[SHOWTIME] No New Version Found! " + ChatColor.RED + tagname + ChatColor.LIGHT_PURPLE + " is the latest version!";
                Bukkit.getConsoleSender().sendMessage(message);
                if (sender != null) {
                    sender.sendMessage(message);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            String message = ChatColor.RED + "[SHOWTIME] Update URL is malformed.";
            Bukkit.getConsoleSender().sendMessage(message);
            if (sender != null) {
                sender.sendMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
            String message = ChatColor.RED + "[SHOWTIME] An error occurred while checking for updates.";
            Bukkit.getConsoleSender().sendMessage(message);
            if (sender != null) {
                sender.sendMessage(message);
            }
        }
    }

}
