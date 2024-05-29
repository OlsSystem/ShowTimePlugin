package net.axolsystems.showtime;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ShowTime extends JavaPlugin implements Listener {

    public static final Logger logger = Logger.getLogger("ASCORE");
    public static ShowTime instance;
    public static String version;
    private Map<String, TeamInfo> teamsMap = new HashMap<>();
    private LuckPerms luckPerms;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updateScoreboard(player.getName());
        updateTabList();
        logger.info("[DEBUG] " + event.getPlayer().getDisplayName() + " Has joined the server!");
    }

    @Override
    public void onEnable() {
        instance = this;
        version = this.getDescription().getVersion();
        logger.info("Enabling ShowTime v" + version);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TeamPlaceholderExpansion(this).register();
        }
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            luckPerms = LuckPermsProvider.get();
        }

        getServer().getPluginManager().registerEvents(this, this);

        Updator.getInstance().autoUpdate();
        initializeTeams();
        updateTabList();
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player.getName());
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(player.getName());
                }
                updateTabList();
                logger.info("Tab List updated automatically!");
            }
        }.runTaskTimer(this, 0L, 20L * 60);
    }

    private void updateScoreboard(String username) {
        // Get the team data from the API
        JSONObject teamData = TeamsAPI.getAllTeamData();
        JSONArray teams = teamData.getJSONArray("teams");

        Player player = Bukkit.getPlayer(username);
        if (player == null) {
            logger.info("Player with username " + username + " is not online.");
            return;
        }

        // Get the scoreboard manager
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            logger.info("Scoreboard manager is not available.");
            return;
        }

        // Create a new scoreboard for this player
        Scoreboard playerScoreboard = manager.getNewScoreboard();

        // Create a new objective for the scoreboard
        Objective objective = playerScoreboard.registerNewObjective("scoreboard", "dummy", ChatColor.GOLD + "Scoreboard");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName("Project Showtime Season 1");

        boolean userFound = false;

        // Add teams to the scoreboard
        for (int i = 0; i < teams.length(); i++) {
            JSONObject team = teams.getJSONObject(i);
            String teamName = team.getString("teamName");
            String colour = team.getString("color");
            int score = team.getInt("points");

            // Create a new team entry or retrieve existing one
            Team scoreboardTeam = playerScoreboard.getTeam(teamName);
            if (scoreboardTeam == null) {
                scoreboardTeam = playerScoreboard.registerNewTeam(teamName);
            }

            // Check online players within this team
            int onlinePlayerCount = 0;
            JSONArray players = team.getJSONArray("players");
            for (int j = 0; j < players.length(); j++) {
                String playerName = players.getString(j);
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.getName().equals(playerName)) {
                        onlinePlayerCount++;
                    }
                }
            }

            // Check if the username is in the team players array
            for (int j = 0; j < players.length(); j++) {
                if (players.getString(j).equals(username)) {
                    userFound = true;

                    String teamNameWithColor = colour + teamName;

                    Score header = objective.getScore(ChatColor.BOLD + "== Your Team ==");
                    header.setScore(10);
                    String teamEntry = ChatColor.translateAlternateColorCodes('&', teamNameWithColor);
                    Score teamNameView = objective.getScore(teamEntry);
                    teamNameView.setScore(1);
                    String scoreEntry = ChatColor.WHITE + "Score: " + ChatColor.GOLD + score;
                    Score teamScoreView = objective.getScore(scoreEntry);
                    teamScoreView.setScore(0);

                }
            }
        }

        // If the user was not found in any team, log and update the scoreboard
        if (!userFound) {
            // Log the username not being on any team
            logger.info("Username " + username + " is not on any team.");

            Score header = objective.getScore(ChatColor.BOLD + "== Your Rank ==");
            header.setScore(10);
            Score noTeamScore = objective.getScore(ChatColor.GOLD.toString() + ChatColor.BOLD + getPlayerPrefix(player));
            noTeamScore.setScore(1); // Setting the score to determine the order
        }

        // Add player count to the scoreboard
        int playerCount = 0;
        for (int i = 0; i < teams.length(); i++) {
            JSONObject team = teams.getJSONObject(i);
            JSONArray players = team.getJSONArray("players");
            for (int j = 0; j < players.length(); j++) {
                String playerName = players.getString(j);
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.getName().equals(playerName)) {
                        playerCount++;
                    }
                }
            }
        }
        Score spacer = objective.getScore(" ");
        spacer.setScore(-1);
        Score playersEntry = objective.getScore("Online Players: " + ChatColor.WHITE + playerCount + "/32");
        playersEntry.setScore(-2); // Setting the score to determine the order

        // Set the player's scoreboard to the new personalized scoreboard
        player.setScoreboard(playerScoreboard);
    }

    public String getPlayerPrefix(Player player) {
        UserManager userManager = luckPerms.getUserManager();
        User user = userManager.getUser(player.getUniqueId());
        if (user != null) {
            String highestGroup = "default"; // Default group if user has no groups
            int highestWeight = -1; // Default weight

            // Retrieve the user's group nodes
            for (String node : user.getNodes().stream()
                    .map(net.luckperms.api.node.Node::getKey) // Extract node keys
                    .collect(Collectors.toList())) {
                // Check if the node represents a group node
                if (node.startsWith("group.")) {
                    String groupName = node.substring("group.".length()); // Extract group name from node
                    return groupName.substring(0, 1).toUpperCase() + groupName.substring(1).toLowerCase();
                }
            }
        }
        return "Default"; // Default group if user is not found
    }

    private void initializeTeams() {
        for (int i = 1; i <= 8; i++) {
            List<String> players = new ArrayList<>(); // Initialize with an empty list of players
            teamsMap.put("Team" + i, new TeamInfo("Team " + i, 0, players));
        }
    }

    public static ShowTime getInstance() {
        return instance;
    }

    public void updateTabList() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String header = ChatColor.GREEN + "PROJECT SHOWTIME\n" +
                    ChatColor.YELLOW + "Presented by " + ChatColor.DARK_AQUA + "OlsSystem" + ChatColor.YELLOW + " & " + ChatColor.AQUA + "SkwSliice\n" +
                    ChatColor.RED + "---------------------------\n" +
                    ChatColor.WHITE + "EVENT STATISTICS:";
            String footer = getTeamData();
            player.setPlayerListHeaderFooter(header, footer);
        }
    }

    private String getTeamData() {
        StringBuilder footer = new StringBuilder();

        try {
            // Retrieve team data from TeamsAPI
            JSONObject teamsObject = TeamsAPI.getAllTeamData();
            JSONArray teamsArray = teamsObject.getJSONArray("teams");

            // List to hold parsed team data
            List<TeamInfo> teamInfoList = new ArrayList<>();

            // Parse team data
            for (int i = 0; i < teamsArray.length(); i++) {
                JSONObject team = teamsArray.getJSONObject(i);
                String teamName = team.getString("teamName");
                int points = team.getInt("points");
                JSONArray playersArray = team.getJSONArray("players");

                List<String> playersList = new ArrayList<>();
                for (int j = 0; j < playersArray.length(); j++) {
                    playersList.add(playersArray.getString(j));
                }

                teamInfoList.add(new TeamInfo(teamName, points, playersList));
            }

            // Sort team data based on points
            Collections.sort(teamInfoList, Comparator.comparingInt(TeamInfo::getPoints).reversed());

            // Determine the fixed widths
            int maxTeamNameLength = teamInfoList.stream()
                    .mapToInt(teamInfo -> teamInfo.getTeamName().length())
                    .max()
                    .orElse(0);

            int teamNameWidth = maxTeamNameLength + 5; // Add some padding
            int pointsWidth = 10; // Adjust this as needed

            // Display team names, players, and points
            for (int i = 0; i < teamInfoList.size(); i++) {
                JSONObject team = teamsArray.getJSONObject(i);
                TeamInfo teamInfo = teamInfoList.get(i);
                String teamName = teamInfo.getTeamName();
                String colour = team.getString("color");
                int points = teamInfo.getPoints();
                List<String> playersList = teamInfo.getPlayers();

                // Construct team name with color
                String teamNameWithColor = colour + teamName;

                // Append position, team name (padded), and points
                footer.append(ChatColor.WHITE)
                        .append(String.format("%d. %-"+teamNameWidth+"s", i + 1, ChatColor.translateAlternateColorCodes('&', teamNameWithColor)))
                        .append(ChatColor.GOLD)
                        .append(String.format("[%d]", points))
                        .append("\n");

                // List players in the team
                for (String playerName : playersList) {
                    Player player = Bukkit.getPlayer(playerName);
                    if (player != null && player.isOnline()) {
                        footer.append(ChatColor.GREEN); // Online players in green
                    } else {
                        footer.append(ChatColor.RED); // Offline players in red
                    }
                    footer.append(playerName).append(", ");
                }
                if (!playersList.isEmpty()) {
                    footer.delete(footer.length() - 2, footer.length()); // Remove the last comma and space
                }

                footer.append('\n');
            }
        } catch (JSONException e) {
            e.printStackTrace();
            footer.append(ChatColor.RED).append("Error retrieving team data.\n");
        }

        return footer.toString();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("addteam")) {
            if (args.length != 7) {
                sender.sendMessage("Usage: /addteam <teamname> <teamname> <color> <player1> <player2> <player3> <player4>");
                return false;
            }
            String teamName = args[0] + " " + args[1];
            String color = args[2];

            String[] players = new String[args.length - 3];
            System.arraycopy(args, 3, players, 0, players.length);

            addTeam(teamName, color, players, sender);
            return true;
        }

        if (command.getName().equalsIgnoreCase("updatedata")) {
            updateTabList();
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player.getName());
            }
            logger.info("Data moduels Updated!");
            sender.sendMessage("Data moduels Updated!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("viewteams")) {
            viewTeams(sender);
            return true;
        }

        if (command.getName().equalsIgnoreCase("addpoints")) {
            if (args.length != 2) {
                sender.sendMessage("Usage: /addpoints <playername> <points>");
                return false;
            }

            String playerName = args[0];
            int pointsToAdd = Integer.parseInt(args[1]);

            addPoints(playerName, pointsToAdd, sender);
            updateTabList();
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player.getName());
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("removepoints")) {
            if (args.length != 2) {
                sender.sendMessage("Usage: /removepoints <playername ><points>");
                return false;
            }

            String playerName = args[0];
            int pointsToRemove = Integer.parseInt(args[1]);

            removePoints(playerName, pointsToRemove, sender);
            updateTabList();
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player.getName());
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("showteam")) {
            if (args.length != 2) {
                sender.sendMessage("Usage: /showteam <teamname> <teamname>");
                return false;
            }

            String teamName = args[0] + " " + args[1];
            showTeam(teamName, sender);
            return true;
        }

        if (command.getName().equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        }

        if (command.getName().equalsIgnoreCase("checkforupdates")) {
            if (sender.hasPermission("showtime.update")) {
                sender.sendMessage(ChatColor.GREEN + "Checking for updates...");
                Updator.getInstance().pluginUpdating(sender, () -> {
                    // Notify when download is completed
                    sender.sendMessage(ChatColor.GREEN + "Plugin update downloaded successfully!");
                });
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
                return true;
            }
        }
        return false;
    }

    private void addPoints(String playerName, int pointsToAdd, CommandSender sender) {
        boolean success = PointsAPI.addPoints(playerName, pointsToAdd);
        if (success) {
            sender.sendMessage("Points added successfully!");
        } else {
            sender.sendMessage("Failed to add points.");
        }
    }

    private void removePoints(String playerName, int pointsToAdd, CommandSender sender) {
        boolean success = PointsAPI.subtractPoints(playerName, pointsToAdd);
        if (success) {
            sender.sendMessage("Points Removed successfully!");
        } else {
            sender.sendMessage("Failed to remove points.");
        }
    }

    private void viewTeams(CommandSender sender) {
        JSONObject teamsObject = TeamsAPI.getAllTeamData();
        if (teamsObject != null) {
            StringBuilder teamsInfo = new StringBuilder("Teams:\n");
            JSONArray teamsArray = teamsObject.getJSONArray("teams"); // Extract the "teams" array from the object
            for (int i = 0; i < teamsArray.length(); i++) {
                JSONObject team = teamsArray.getJSONObject(i);
                String teamName = team.getString("teamName");
                String color = team.getString("color");
                JSONArray players = team.getJSONArray("players");

                teamsInfo.append("Team: ").append(teamName).append(",\n Color: ").append(color).append(",\n Players: ");
                for (int j = 0; j < players.length(); j++) {
                    teamsInfo.append(players.getString(j));
                    if (j < players.length() - 1) {
                        teamsInfo.append(", ");
                    }
                }
                teamsInfo.append("\n");
            }
            sender.sendMessage(teamsInfo.toString());
        } else {
            sender.sendMessage("Failed to retrieve team data.");
        }
    }

    private void showTeam(String teamName, CommandSender sender) {
        JSONObject teamsObject = TeamsAPI.getAllTeamData();
        if (teamsObject == null) {
            sender.sendMessage("Failed to retrieve team data.");
            return;
        }

        JSONArray teams = teamsObject.getJSONArray("teams");
        for (int i = 0; i < teams.length(); i++) {
            JSONObject team = teams.getJSONObject(i);
            if (team.getString("teamName").equalsIgnoreCase(teamName)) {
                sender.sendMessage("Team: " + team.getString("teamName"));
                sender.sendMessage("Points: " + team.getInt("points"));
                JSONArray players = team.getJSONArray("players");
                StringBuilder playerList = new StringBuilder();
                for (int j = 0; j < players.length(); j++) {
                    playerList.append(players.getString(j));
                    if (j < players.length() - 1) {
                        playerList.append(", ");
                    }
                }
                sender.sendMessage("Players: " + playerList.toString());
                return;
            }
        }
        sender.sendMessage("Team not found.");
    }

    private void addTeam(String teamName, String color, String[] players, CommandSender sender) {
        boolean success = TeamsAPI.addTeam(teamName, color, players);
        if (success) {
            sender.sendMessage("Team added successfully!");
        } else {
            sender.sendMessage("Failed to add team.");
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("Available Commands:");
        sender.sendMessage("/addteam <teamname> <teamname> <color> <player1> <player2> <player3> <player4>");
        sender.sendMessage("/viewteams");
        sender.sendMessage("/addpoints <teamname> <teamname> <points>");
        sender.sendMessage("/removepoints <teamname> <teamname> <points>");
        sender.sendMessage("/showteam <teamname> <teamname>");
        sender.sendMessage("/help");
        sender.sendMessage("/checkforupdates");
    }
}
