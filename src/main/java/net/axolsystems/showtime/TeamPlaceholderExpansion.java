package net.axolsystems.showtime;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

public class TeamPlaceholderExpansion extends PlaceholderExpansion {

    private final ShowTime plugin;
    private final TeamsAPI teamsAPI;

    public TeamPlaceholderExpansion(ShowTime plugin) {
        this.plugin = plugin;
        this.teamsAPI = new TeamsAPI();
    }

    @Override
    public boolean persist() {
        return true; // This means you don't have to register it again after a reload
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "team";
    }

    @Override
    public String getAuthor() {
        return "YourName";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (identifier.startsWith("points_")) {
            String teamName = identifier.split("_")[1];
            try {
                String teamDataJson = String.valueOf(TeamsAPI.getAllTeamData());
                JSONArray teamArray = new JSONArray(teamDataJson);
                for (int i = 0; i < teamArray.length(); i++) {
                    JSONObject team = teamArray.getJSONObject(i);
                    if (team.getString("teamName").equalsIgnoreCase(teamName)) {
                        return String.valueOf(team.getInt("points"));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "0"; // Team not found or error occurred
        }
        return null; // Placeholder not recognized
    }
}
