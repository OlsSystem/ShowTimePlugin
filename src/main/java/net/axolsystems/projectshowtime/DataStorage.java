package net.axolsystems.projectshowtime;

import java.util.List;

public class DataStorage {
    private String teamName;
    private String color;
    private List<String> players;
    private int points;

    // Empty constructor required for Firestore deserialization
    public DataStorage() {
    }

    public DataStorage(String teamName, String color, List<String> players, int points) {
        this.teamName = teamName;
        this.color = color;
        this.players = players;
        this.points = points;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<String> getPlayers() {
        return players;
    }

    public void setPlayers(List<String> players) {
        this.players = players;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
