package net.axolsystems.showtime;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class CutsceneManager {
    private final Map<String, CutsceneInfo> cutscenes = new HashMap<>();
    private final ShowTime plugin;

    public CutsceneManager(ShowTime plugin) {
        this.plugin = plugin;
    }

    public void createCutscene(String name) {
        cutscenes.put(name, new CutsceneInfo(name));
    }

    public CutsceneInfo getCutscene(String name) {
        return cutscenes.get(name);
    }

    public void playCutscene(Player player, String name) {
        CutsceneInfo cutscene = cutscenes.get(name);

        if (cutscene == null) {
            player.sendMessage("The cutscene " + name + " was not found.");
            return;
        }

        GameMode originalGameMode = player.getGameMode();
        player.setGameMode(GameMode.SPECTATOR);

        new BukkitRunnable() {
            int stepIndex = 0;

            @Override
            public void run() {
                if (stepIndex >= cutscene.getSteps().size()) {
                    player.sendMessage("Cutscene " + name + " finished.");
                    player.setGameMode(originalGameMode);
                    cancel();
                    return;
                }

                CutsceneInfo.CameraStep currentStep = cutscene.getSteps().get(stepIndex);

                if (stepIndex + 1 < cutscene.getSteps().size()) {
                    CutsceneInfo.CameraStep nextStep = cutscene.getSteps().get(stepIndex + 1);
                    smoothMove(player, currentStep.getLocation(), nextStep.getLocation(), currentStep.getDuration());
                } else {
                    sendPositionPacket(player, currentStep.getLocation());
                }

                stepIndex++;

                if (stepIndex < cutscene.getSteps().size()) {
                    this.runTaskLater(plugin, currentStep.getDuration());
                } else {
                    player.setGameMode(originalGameMode);
                    cancel();
                }
            }
        }.runTask(plugin);
    }

    public void sendPositionPacket(Player player, Location location) {
        PacketContainer packet = ShowTime.getProtocolManager().createPacket(PacketType.Play.Server.POSITION);

        packet.getDoubles().write(0, location.getX()).write(1, location.getY()).write(2, location.getZ());
        packet.getFloat().write(0, location.getYaw()).write(1, location.getPitch());

        try {
            ShowTime.getProtocolManager().sendServerPacket(player, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void smoothMove(Player player, Location start, Location end, int duration) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                double progress = (double) ticks / duration;
                double x = start.getX() + (end.getX() - start.getX()) * progress;
                double y = start.getY() + (end.getY() - start.getY()) * progress;
                double z = start.getZ() + (end.getZ() - start.getZ()) * progress;
                float yaw = start.getYaw() + (end.getYaw() - start.getYaw()) * (float) progress;
                float pitch = start.getPitch() + (end.getPitch() - start.getPitch()) * (float) progress;

                sendPositionPacket(player, new Location(start.getWorld(), x, y, z, yaw, pitch));
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}