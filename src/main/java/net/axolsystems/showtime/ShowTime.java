package net.axolsystems.showtime;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;
import org.json.JSONArray;
import org.json.JSONObject;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.query.QueryOptions;

public class ShowTime extends JavaPlugin implements Listener {

    public static final Logger logger = Logger.getLogger("ASCORE");
    public static ShowTime instance;
    public static String version;

    private Map<String, TeamInfo> teamsMap = new HashMap<>();
    private LuckPerms luckPerms;
    private Set<Location> launcherBlocks;
    private File launcherFile;
    private FileConfiguration launcherConfig;

    private final Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");

    public static ShowTime getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        version = this.getDescription().getVersion();

        logger.log(Level.INFO, "ShowTime v{0} loading...", version);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceHolderAdditions(this).register();
        }

        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            luckPerms = LuckPermsProvider.get();
        }

        getServer().getPluginManager().registerEvents(this, this);

        launcherBlocks = new HashSet<>();
        launcherFile = new File(getDataFolder(), "launchers.yml");
        launcherConfig = YamlConfiguration.loadConfiguration(launcherFile);

        ShowTimeUpdate.getInstance().autoUpdate();

        //loadLaunchers();
        initializeTeams();
        updateTabList();

        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player.getName());
        }


    }

    @Override
    public void onDisable() {
        //saveLaunchers();
        logger.log(Level.WARNING, "ShowTime v{0} is disabling.", version);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updateScoreboard(player.getName());
        updateTabList();
        JSONObject settingsData = ShowTimeAPI.apiGetMethod("/api/getSettings");
        JSONObject settings = settingsData.getJSONObject("systemsData");
        String gameName = settings.getString("gameName");

        logger.log(Level.INFO, "Game Name: {0}", gameName);
        logger.log(Level.INFO, "[DEBUG] {0} Has Joined the server!", event.getPlayer().getDisplayName());
    }
    
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        updateScoreboard(player.getName());
        updateTabList();

        logger.log(Level.INFO, "[DEBUG]{0} Has Left the server.", event.getPlayer().getDisplayName());
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        String prefix = user.getCachedData().getMetaData(QueryOptions.defaultContextualOptions()).getPrefix();
        prefix = (prefix == null) ? "§7§lDefault " : prefix;

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
                double forwardFactor = 7.5;
                Vector forwardVector = boat.getLocation().getDirection().normalize();
                Vector newVelocity = boat.getVelocity().add(forwardVector.multiply(forwardFactor)).add(new Vector(0, 1.5, 0));
                boat.setVelocity(newVelocity);
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
        
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', prefix);
    }

    private String getPositionSuffix(int position) {
        if (position == 1) {
            return "st";
        } else if (position == 2) {
            return "nd";
        } else if (position == 3) {
            return "rd";
        } else {
            return "th";
        }
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
            String highestGroup = "default";
            int highestWeight = -1;

            for (String node : user.getNodes().stream().map(net.luckperms.api.node.Node::getKey).collect(Collectors.toList())) {

                if (node.startsWith("group.")) {
                    String groupName = node.substring("group.".length());
                    return groupName.substring(0, 1).toUpperCase() + groupName.substring(1).toLowerCase();
                }
            }
        }
        return "Default";
    }

    private void initializeTeams() {
        for ( int i = 1; i <= 8; i++) {
            List<String> players = new ArrayList<>();
            teamsMap.put("Team" + i, new TeamInfo("Team " + i, 0, players, "RED", "default"));
        }
    }

    public void updateTabList() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            //String header = ChatColor.GREEN.toString() + ChatColor.BOLD + "PROJECT SHOWTIME\n" + ChatColor.YELLOW + "Presented By " + ChatColor.DARK_AQUA + "OlsSystem" + ChatColor.YELLOW + "&" + ChatColor.AQUA + "SkwSliice\n" + ChatColor.RED + "---------------------------\n" + ChatColor.WHITE + "EVENT STATISTICS:" + "\n\n" + getTeamData();

            //player.setPlayerListHeader(header);
        }
    }

    private void updateScoreboard(String username) {
        
        JSONObject teamData = ShowTimeAPI.apiGetMethod("/api/teamdata");
        JSONObject settingsData = ShowTimeAPI.apiGetMethod("/api/getSettings");
        JSONObject settings = settingsData.getJSONObject("systemsData");
        JSONArray teams = teamData.getJSONArray("teams");
        JSONArray pointsArray = settingsData.getJSONArray("pointsArray");
        
        Boolean inRound = settings.getBoolean("inRound");

        Player player = Bukkit.getPlayer(username);
        if (player == null) {
            logger.log(Level.INFO, "Player with the username {0} is not online.", username);
            return;
        }

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            logger.log(Level.INFO, "Soreboard manager is not avaliable currently.");
        }

        Scoreboard playerScoreboard = manager.getNewScoreboard();
        Objective objective = playerScoreboard.registerNewObjective("scoreboard", "dummy", ChatColor.GOLD + "Scoreboard");

        if (inRound) {

            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.setDisplayName(ChatColor.RED.toString() + ChatColor.BOLD  + "Project Showtime Season 1");

            Score divider = objective.getScore(ChatColor.DARK_GRAY + "---------------------------");
            divider.setScore(16);

            Score gameInfo = objective.getScore(ChatColor.YELLOW + "Game: " + ChatColor.WHITE + settings.getInt("gamePos") + "/6: " + settings.getString("gameName"));
            gameInfo.setScore(15);
            Score roundInfo = objective.getScore(ChatColor.YELLOW + "Round: " + ChatColor.WHITE + settings.getInt("roundNumber") + "/" + settings.getInt("maxRounds"));
            roundInfo.setScore(14);
            Score mapInfo = objective.getScore(ChatColor.YELLOW + "Map: " + ChatColor.WHITE + settings.getString("mapName"));
            mapInfo.setScore(13);

            List<JSONObject> teamList  = new ArrayList<>();
            for (int i = 0; i < pointsArray.length(); i++) {
                JSONObject teamPoint = pointsArray.getJSONObject(i);
                String teamName = teamPoint.getString("team");
                int points = teamPoint.getInt("points");

                for (int j = 0; j < teams.length(); j++) {
                    JSONObject team = teams.getJSONObject(j);

                    if (team.getString("teamName").equals(teamName)) {
                        team.put("points", points);
                        teamList.add(team);
                        break;
                    }
                }
            }

            teamList.sort((a, b) -> Integer.compare(b.getInt("points"), a.getInt("points")));

            for (int i = 0; i < Math.min(teamList.size(), 3); i++) {
                JSONObject team = teamList.get(i);
                String teamName = team.getString("teamName");
                int score = team.getInt("points");

                Score scoreEntry = objective.getScore((i + 1) + getPositionSuffix(i + 1) + ": " + teamName + " - " + score);
                scoreEntry.setScore(11 - i);
            }

            Score header = objective.getScore(ChatColor.DARK_GRAY + "---------------------------");
            header.setScore(8);

            boolean userFound = false;

            for (int i = 0; i < teamList.size(); i++) {
                JSONObject team = teamList.get(i);
                JSONArray players = team.getJSONArray("players");

                for (int j = 0; j < players.length(); j++) {
                    if (players.getString(j).equals(username)) {
                        String positionSuffix = getPositionSuffix(i + 1);
                        Score userTeamPosScore = objective.getScore((i + 1) + positionSuffix + ": " + team.getString("teamName") + " - " + team.getInt("points") + " ");
                        userTeamPosScore.setScore(7);
                        userFound = true;
                        break;
                    }
                }
                if (userFound) {
                    break;
                }
            }

            player.setScoreboard(playerScoreboard);
        } else {

            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.setDisplayName(ChatColor.RED.toString() + ChatColor.BOLD + "Project Showtime Season1");

            boolean userFound = false;

            for (int i = 0; i < teams.length(); i++) {
                JSONObject team = teams.getJSONObject(i);
                String teamName = team.getString("teamName");
                String colour = team.getString("color");
                int score = team.getInt("points");
                String lpGroup = team.getString("lpGroup");
                JSONArray players = team.getJSONArray("players");

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

                        Team minecraftTeam = playerScoreboard.getTeam(lpGroup);
                        if (minecraftTeam == null) {
                            minecraftTeam = playerScoreboard.registerNewTeam(lpGroup);
                            minecraftTeam.setDisplayName(teamName);
                            minecraftTeam.setColor(getChatColor(colour));
                            logger.log(Level.WARNING, "Created new minecraft team for {0}", teamName);
                        }
                        minecraftTeam.addEntry(player.getName());
                    
                    }
                }
            }

            if (!userFound) {

                logger.log(Level.INFO, "User {0} is not apart of any team.", username);

                Score header = objective.getScore(ChatColor.BOLD + "== Your Rank ==");
                header.setScore(10);
                Score noTeamScore = objective.getScore(ChatColor.GOLD.toString() + ChatColor.BOLD + getPlayerPrefix(player));
                noTeamScore.setScore(1);
            }

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
            playersEntry.setScore(-2);

            player.setScoreboard(playerScoreboard);
        }
    }
}