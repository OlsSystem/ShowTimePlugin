package net.axolsystems.showtime;

import java.util.List;

public class TeamInfo {
    private String teamName;
    private int points;
    private String color;
    private String lpgroup;
    private List<String> players;

    public TeamInfo(String teamName, int points, List<String> players, String color, String lpgroup) {
        this.teamName = teamName;
        this.points = points;
        this.players = players;
        this.color = color;
        this.lpgroup = lpgroup;
    }


    public String getTeamName() {
        return teamName;
    }

    public String getTeamColor() {
        return color;
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
