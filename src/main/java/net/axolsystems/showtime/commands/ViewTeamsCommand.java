package net.axolsystems.showtime.commands;

import net.axolsystems.showtime.ShowTimeAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

public class ViewTeamsCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public ViewTeamsCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        JSONObject teamsObject = ShowTimeAPI.apiGetMethod("/api/teamdata");

        if (teamsObject != null) {
            StringBuilder teamsInfo = new StringBuilder("Teams:\n");
            JSONArray teamsArray = teamsObject.getJSONArray("teams");

            for (int i = 0; i < teamsArray.length(); i++) {
                JSONObject team = teamsArray.getJSONObject(i);
                String teamName = team.getString("teamName");
                String colour = team.getString("color");
                JSONArray teamMembers = team.getJSONArray("players");
                teamsInfo.append("Team: ").append(teamName).append(",\n Colour: ").append(colour).append(",\n Contestants: ");

                for (int j = 0; i < teamMembers.length(); i++) {
                    teamsInfo.append(teamMembers.getString(j));
                    if (j < teamMembers.length() - 1) {
                        teamsInfo.append(", ");
                    }
                }
                teamsInfo.append("\n");
            }
            sender.sendMessage(teamsInfo.toString());
            return true;
        } else {
            sender.sendMessage("Failed to fetch all Team Data.");
            return false;
        }
    }
}
