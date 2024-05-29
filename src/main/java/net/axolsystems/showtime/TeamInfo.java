package net.axolsystems.showtime;

import java.util.List;

public class TeamInfo {
    private String teamName;
    private int points;
    private List<String> players;

    public TeamInfo(String teamName, int points, List<String> players) {
        this.teamName = teamName;
        this.points = points;
        this.players = players;
    }


    public String getTeamName() {
        return teamName;
    }

    public int getPoints() {
        return points;
    }

    public List<String> getPlayers() {
        return players;
    }
}
