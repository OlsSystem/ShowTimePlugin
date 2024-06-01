package net.axolsystems.showtime;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ShowTime extends JavaPlugin implements Listener {

    public static final Logger logger = Logger.getLogger("ASCORE");
    public static ShowTime instance;
    public static String version;
    private Map<String, TeamInfo> teamsMap = new HashMap<>();
    private LuckPerms luckPerms;
    private final Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
    private Set<Location> launcherBlocks;
    private File launcherFile;
    private FileConfiguration launcherConfig;


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
        launcherBlocks = new HashSet<>();
        launcherFile = new File(getDataFolder(), "launchers.yml");
        launcherConfig = YamlConfiguration.loadConfiguration(launcherFile);
        loadLaunchers();

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
                    if (Bukkit.getOnlinePlayers() == null) return;
                    updateScoreboard(player.getName());
                }
                updateTabList();
                logger.info("Tab List updated automatically!");

            }
        }.runTaskTimer(this, 0L, 20L * 60);
    }

    @Override
    public void onDisable() {
        saveLaunchers();
        logger.info("Disabling ShowTime v" + version);
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updateScoreboard(player.getName());
        updateTabList();
        logger.info("[DEBUG] " + event.getPlayer().getDisplayName() + " Has joined the server!");
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        updateScoreboard(player.getName());
        updateTabList();
        logger.info("[DEBUG] " + event.getPlayer().getDisplayName() + " Has Left the server!");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        String prefix = user.getCachedData().getMetaData(QueryOptions.defaultContextualOptions()).getPrefix();

        String formattedPrefix = formattedPrefix(prefix);
        String message = formattedPrefix + player.getDisplayName() + "§r: " + formattedPrefix(event.getMessage());
        event.setFormat(message);
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (event.getVehicle() instanceof Boat) {
            Boat boat = (Boat) event.getVehicle();
            Location boatLocation = boat.getLocation();
            Location blockLocation = boatLocation.getBlock().getRelative(0, -1, 0).getLocation();

            if (launcherBlocks.contains(blockLocation)) {
                // Directly apply an upward velocity change to the boat
                boat.setVelocity(boat.getVelocity().add(new Vector(0, 2, 0)));
            }
        }
    }


    private String formattedPrefix(String prefix) {
        Matcher match = pattern.matcher(prefix);
        while (match.find()) {
            String color = prefix.substring(match.start(), match.end());
            prefix = prefix.replace(color, net.md_5.bungee.api.ChatColor.of(color) + "");
            match = pattern.matcher(prefix);
        }
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&',prefix);
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
        objective.setDisplayName(ChatColor.RED.toString() + ChatColor.BOLD + "Project Showtime Season 1");

        boolean userFound = false;

        // Add teams to the scoreboard
        for (int i = 0; i < teams.length(); i++) {
            JSONObject team = teams.getJSONObject(i);
            String teamName = team.getString("teamName");
            String colour = team.getString("color");
            int score = team.getInt("points");
            String lpGroup = team.getString("lpGroup");

            // Create a new team entry or retrieve existing one
            Team scoreboardTeam = playerScoreboard.getTeam(teamName);
            if (scoreboardTeam == null) {
                scoreboardTeam = playerScoreboard.registerNewTeam(teamName);
            }



            // Makes teams for main board innit wag wan
            Scoreboard mainScoreboard = manager.getMainScoreboard();
            Team minecraftTeam = mainScoreboard.getTeam(lpGroup);
            if (minecraftTeam == null) {
                minecraftTeam = mainScoreboard.registerNewTeam(lpGroup);
                minecraftTeam.setDisplayName(teamName);
                minecraftTeam.setColor(getChatColor(colour));
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

                    String teamNameWithColor = colour + "&l" + teamName;

                    Score header = objective.getScore(ChatColor.BOLD + "== Your Team ==");
                    header.setScore(10);
                    String teamEntry = ChatColor.translateAlternateColorCodes('&', teamNameWithColor);
                    Score teamNameView = objective.getScore(teamEntry);
                    teamNameView.setScore(1);
                    String scoreEntry = ChatColor.WHITE + "Score: " + ChatColor.GOLD + score;
                    Score teamScoreView = objective.getScore(scoreEntry);
                    teamScoreView.setScore(0);

                    try {
                        CompletableFuture<User> userFuture = luckPerms.getUserManager().loadUser(player.getUniqueId());

                        userFuture.thenAcceptAsync(user -> {
                            InheritanceNode node = InheritanceNode.builder(lpGroup).build();
                            user.data().add(node);
                            luckPerms.getUserManager().saveUser(user);
                        });
                    } catch (Exception e) {
                        return;
                    }

                    try {
                        minecraftTeam.addEntry(player.getName());
                    } catch (Exception e) {
                        return;
                    }
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

    private ChatColor getChatColor(String colorCode) {
        switch (colorCode.toLowerCase()) {
            case "&0": return ChatColor.BLACK;
            case "&1": return ChatColor.DARK_BLUE;
            case "&2": return ChatColor.DARK_GREEN;
            case "&3": return ChatColor.DARK_AQUA;
            case "&4": return ChatColor.DARK_RED;
            case "&5": return ChatColor.DARK_PURPLE;
            case "&6": return ChatColor.GOLD;
            case "&7": return ChatColor.GRAY;
            case "&8": return ChatColor.DARK_GRAY;
            case "&9": return ChatColor.BLUE;
            case "&a": return ChatColor.GREEN;
            case "&b": return ChatColor.AQUA;
            case "&c": return ChatColor.RED;
            case "&d": return ChatColor.LIGHT_PURPLE;
            case "&e": return ChatColor.YELLOW;
            case "&f": return ChatColor.WHITE;
            default: throw new IllegalArgumentException("Invalid color code: " + colorCode);
        }
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
            teamsMap.put("Team" + i, new TeamInfo("Team " + i, 0, players, "RED", "default"));
        }
    }

    public static ShowTime getInstance() {
        return instance;
    }

    public void updateTabList() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String header = ChatColor.GREEN.toString() + ChatColor.BOLD + "PROJECT SHOWTIME\n" +
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
                String teamColor = team.getString("color");
                String lpGroupName = team.getString("lpGroup");
                int points = team.getInt("points");
                JSONArray playersArray = team.getJSONArray("players");

                List<String> playersList = new ArrayList<>();
                for (int j = 0; j < playersArray.length(); j++) {
                    playersList.add(playersArray.getString(j));
                }

                teamInfoList.add(new TeamInfo(teamName, points, playersList, teamColor, lpGroupName));
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
                String colour = teamInfo.getTeamColor();
                int points = teamInfo.getPoints();
                List<String> playersList = teamInfo.getPlayers();

                // Construct team name with color
                String teamNameWithColor = colour + "&l" + teamName;

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

        if (command.getName().equalsIgnoreCase("markslimelauncher")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                Location targetBlockLocation = player.getTargetBlockExact(5).getLocation(); // Get block the player is looking at (within 5 blocks)
                if (targetBlockLocation != null && targetBlockLocation.getBlock().getType() == Material.SLIME_BLOCK) {
                    launcherBlocks.add(targetBlockLocation);
                    player.sendMessage("This slime block is now a launcher!");
                    return true;
                } else {
                    player.sendMessage("You must be looking at a slime block to mark it as a launcher.");
                    return false;
                }
            } else {
                sender.sendMessage("This command can only be used by players.");
                return false;
            }
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

    private void loadLaunchers() {
        if (launcherConfig.contains("launchers")) {
            for (String key : launcherConfig.getConfigurationSection("launchers").getKeys(false)) {
                Location loc = launcherConfig.getLocation("launchers." + key);
                if (loc != null) {
                    launcherBlocks.add(loc);
                }
            }
        }
    }

    private void saveLaunchers() {
        int i = 0;
        for (Location loc : launcherBlocks) {
            launcherConfig.set("launchers." + i, loc);
            i++;
        }
        try {
            launcherConfig.save(launcherFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
