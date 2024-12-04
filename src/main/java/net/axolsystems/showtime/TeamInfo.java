package net.axolsystems.showtime;

import java.util.List;

public class TeamInfo {
    private String teamName;
    private int points;
    private String colour;
    private String lpgroup;
    private List<String> players;

    public TeamInfo(String teamName, int points, List<String> players, String colour, String lpgroup) {
        this.teamName = teamName;
        this.points = points;
        this.colour = colour;
        this.lpgroup = lpgroup;
        this.players = players;
    }

    public String getTeamName() {
        return teamName;
    }
    
    public String getTeamColour() {
        return colour;
    }

    public String getLpgroup() {
        return lpgroup;
    }

    public int getPoints() {
        return points;
    }

    public List<String> getPlayers() {
        return players;
    }
    
}
