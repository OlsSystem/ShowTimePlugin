package net.axolsystems.showtime;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import org.json.JSONArray;
import org.json.JSONObject;

public class PlaceHolderAdditions extends PlaceholderExpansion {

    private final ShowTime plugin;
    private final ShowTimeAPI showTimeAPI;

    public PlaceHolderAdditions(ShowTime plugin) {
        this.plugin = plugin;
        this.showTimeAPI = new ShowTimeAPI();
    }

    @Override
    public boolean persist() {
        return true;
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

    public JSONObject getAllTeamData() {
        return showTimeAPI.apiGetMethod("/api/teamdata");
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (identifier.startsWith("points_")) {
            String teamName = identifier.split("_")[1];
            
            try {
                String teamDataJson = String.valueOf(getAllTeamData());
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
            return "0";
        }

        return null;
    }


    
}
