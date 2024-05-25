package net.axolsystems.projectshowtime;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.DocumentSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ShowTime extends JavaPlugin {

    private Firestore firestore;

    @Override
    public void onEnable() {
        initializeFirebase();
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TeamPlaceholderExpansion(this).register();
        }
    }

    private void initializeFirebase() {
        try {
            File file = new File("D:/Projects/Project ShowTime/Plugin/src/main/resources/config/serviceAccount.json");
            System.out.println("Absolute path: " + file.getAbsolutePath());
            FileInputStream serviceAccount = new FileInputStream("D:/Projects/Project ShowTime/Plugin/src/main/resources/config/serviceAccount.json");
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://project-showtime-f6e40.firebaseio.com")
                    .build();
            FirebaseApp.initializeApp(options);
            firestore = FirestoreClient.getFirestore();
        } catch (IOException e) {
            getLogger().severe("Failed to initialize Firebase: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("addteam")) {
            if (args.length != 7) {
                sender.sendMessage("Usage: /addteam <teamname> <teamname> <color> <player1> <player2> <player3> <player4>");
                return false;
            }

            String teamName = args[0] + " " + args[1];
            String color = args[2].toUpperCase();
            String player1 = args[3];
            String player2 = args[4];
            String player3 = args[5];
            String player4 = args[6];

            if (!isValidColor(color)) {
                sender.sendMessage("Invalid color. Please use a valid ChatColor.");
                return false;
            }

            addTeam(teamName, color, Arrays.asList(player1, player2, player3, player4), sender);
            return true;
        }

        if (command.getName().equalsIgnoreCase("viewteams")) {
            viewTeams(sender);
            return true;
        }

        if (command.getName().equalsIgnoreCase("addpoints")) {
            if (args.length != 3) {
                sender.sendMessage("Usage: /addpoints <teamname> <teamname> <points>");
                return false;
            }

            String teamName = args[0] + " " + args[1];
            int pointsToAdd = Integer.parseInt(args[2]);

            addPoints(teamName, pointsToAdd, sender);
            return true;
        }

        if (command.getName().equalsIgnoreCase("removepoints")) {
            if (args.length != 3) {
                sender.sendMessage("Usage: /removepoints <teamname> <teamname> <points>");
                return false;
            }

            String teamName = args[0] + " " + args[1];
            int pointsToRemove = Integer.parseInt(args[2]);

            removePoints(teamName, pointsToRemove, sender);
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

        if (command.getName().equalsIgnoreCase("title")) {
            if (args.length != 3) {
                sender.sendMessage("Usage: /title <player> <teamname> <teamname>");
                return false;
            }

            String playerName = args[0];
            String teamName = args[1] + " " + args[2];

            sendTeamTitle(playerName, teamName, sender);
            return true;
        }
        if (command.getName().equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        }
        return false;
    }


    private void addTeam(String teamName, String color, List<String> players, CommandSender sender) {
        DocumentReference teamRef = firestore.collection("teams").document(teamName);
        DataStorage team = new DataStorage(teamName, color, players, 0);
        ApiFuture<WriteResult> result = teamRef.set(team);
        try {
            result.get(); // This blocks until the operation completes
            sender.sendMessage("Team added successfully!");
        } catch (Exception e) {
            sender.sendMessage("Failed to add team: " + e.getMessage());
        }
    }

    private void viewTeams(CommandSender sender) {
        ApiFuture<QuerySnapshot> future = firestore.collection("teams").get();
        ApiFutures.addCallback(future, new ApiFutureCallback<QuerySnapshot>() {
            @Override
            public void onFailure(Throwable throwable) {
                sender.sendMessage("Failed to retrieve teams from database.");
            }

            @Override
            public void onSuccess(QuerySnapshot querySnapshot) {
                for (QueryDocumentSnapshot document : querySnapshot) {
                    String teamName = document.getString("teamName");
                    String color = document.getString("color");
                    Long points = document.getLong("points");
                    List<String> players = (List<String>) document.get("players");

                    sender.sendMessage("Team: " + teamName);
                    sender.sendMessage("Color: " + color);
                    sender.sendMessage("Players: " + String.join(", ", players));
                    sender.sendMessage("Points: " + points);
                    sender.sendMessage("-----");
                }
            }
        }, MoreExecutors.directExecutor());
    }

    private void addPoints(String teamName, int pointsToAdd, CommandSender sender) {
        DocumentReference teamRef = firestore.collection("teams").document(teamName);
        ApiFuture<WriteResult> future = teamRef.update("points", FieldValue.increment(pointsToAdd));
        ApiFuture<Void> transformedFuture = ApiFutures.transformAsync(future,
                result -> {
                    return ApiFutures.immediateFuture(null); // Convert to ApiFuture<Void>
                }, MoreExecutors.directExecutor());

        ApiFutures.addCallback(transformedFuture, new ApiFutureCallback<Void>() {
            @Override
            public void onFailure(Throwable throwable) {
                sender.sendMessage("Failed to add points: " + throwable.getMessage());
            }

            @Override
            public void onSuccess(Void aVoid) {
                sender.sendMessage("Points added successfully!");
            }
        }, MoreExecutors.directExecutor());
    }

    private void removePoints(String teamName, int pointsToRemove, CommandSender sender) {
        DocumentReference teamRef = firestore.collection("teams").document(teamName);
        ApiFuture<WriteResult> future = teamRef.update("points", FieldValue.increment(-pointsToRemove));
        ApiFuture<Void> transformedFuture = ApiFutures.transformAsync(future,
                result -> {
                    return ApiFutures.immediateFuture(null); // Convert to ApiFuture<Void>
                }, MoreExecutors.directExecutor());

        ApiFutures.addCallback(transformedFuture, new ApiFutureCallback<Void>() {
            @Override
            public void onFailure(Throwable throwable) {
                sender.sendMessage("Failed to remove points: " + throwable.getMessage());
            }

            @Override
            public void onSuccess(Void aVoid) {
                sender.sendMessage("Points removed successfully!");
            }
        }, MoreExecutors.directExecutor());
    }

    private void showTeam(String teamName, CommandSender sender) {
        DocumentReference teamRef = firestore.collection("teams").document(teamName);
        ApiFuture<DocumentSnapshot> future = teamRef.get();
        ApiFutures.addCallback(future, new ApiFutureCallback<DocumentSnapshot>() {
            @Override
            public void onFailure(Throwable throwable) {
                sender.sendMessage("Failed to retrieve team data: " + throwable.getMessage());
            }

            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    DataStorage team = documentSnapshot.toObject(DataStorage.class);
                    if (team != null) {
                        sender.sendMessage("Team " + teamName + " has " + team.getPoints() + " points.");
                    } else {
                        sender.sendMessage("Team data is null.");
                    }
                } else {
                    sender.sendMessage("Team not found.");
                }
            }
        }, MoreExecutors.directExecutor());
    }

    private void sendTeamTitle(String playerName, String teamName, CommandSender sender) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage("Player not found.");
            return;
        }
        DocumentReference teamRef = firestore.collection("teams").document(teamName);
        ApiFuture<DocumentSnapshot> future = teamRef.get();
        ApiFutures.addCallback(future, new ApiFutureCallback<DocumentSnapshot>() {
            @Override
            public void onFailure(Throwable throwable) {
                sender.sendMessage("Failed to retrieve team data: " + throwable.getMessage());
            }

            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    DataStorage team = documentSnapshot.toObject(DataStorage.class);
                    if (team != null) {
                        String color = team.getColor();
                        int points = team.getPoints();
                        List<String> players = team.getPlayers();

                        player.sendTitle(
                                ChatColor.valueOf(color) + "Team " + team.getTeamName(),
                                "Points: " + points + "\nPlayers: " + String.join(", ", players),
                                10, 70, 20
                        );
                    } else {
                        sender.sendMessage("Team data is null.");
                    }
                } else {
                    sender.sendMessage("Team not found.");
                }
            }
        }, MoreExecutors.directExecutor());
    }


    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("Available Commands:");
        sender.sendMessage("/addteam <teamname> <teamname> <color> <player1> <player2> <player3> <player4>");
        sender.sendMessage("/viewteams");
        sender.sendMessage("/addpoints <teamname> <teamname> <points>");
        sender.sendMessage("/removepoints <teamname> <teamname> <points>");
        sender.sendMessage("/showteam <teamname> <teamname>");
        sender.sendMessage("/title <player> <teamname> <teamname>");
        sender.sendMessage("/help");
    }

    private boolean isValidColor(String color) {
        try {
            ChatColor.valueOf(color);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
