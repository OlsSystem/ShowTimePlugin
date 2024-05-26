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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShowTime extends JavaPlugin {

    private static final Logger logger = Logger.getLogger(ShowTime.class.getName());
    private Firestore firestore;

    @Override
    public void onEnable() {
        logger.info("showtimeplugin is being enabled...");
        initializeFirebase();
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TeamPlaceholderExpansion(this).register();
            logger.info("PlaceholderAPI found and registered.");
        }
    }

    private void initializeFirebase() {
        try {
            // Hardcoded JSON string for the service account
            String serviceAccountJson = "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"project-showtime-f6e40\",\n" +
                    "  \"private_key_id\": \"6d3046e6e837a135a1b08e697a560fc4d1764537\",\n" +
                    "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\nMIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCahZ/kBdgtqbRz\neRjiUs13wSSgSE7aO779FpKB6n1Gy+hOWx9jIkE4nrz1tjD6Rb7Hfk5lM4/wgvaK\nkI4Q5Mwkrjc9dGpSdndZuFd+cNJ1q4sJidZY3MGRS4TvzGRVx/nqZIs/BzVDg6Ep\n7slJkOASUM5xfKBPITbYjjLiucAcnKR9S2p/IpLAepwrceAaxHE4evg2CE1LYUqF\nJbMqqkMUBYUd81ZjfZWFg0aBg05j+Hl0sw6W2ymAYbT8+Slrqm80xteJQSsdH2C0\nPBYsnruXFeFv5d8Eob8Odm7BSFPqv9vVY5fTCd3TqwwY2KVsK73BSrFG0O9SwFBA\nA2xhe79XAgMBAAECggEARAQgir7V1te18gQyY2D+R0HRrOnXJ7GX0pCOgbglV7Xm\ntiLfEIRiABnDZEbDjU6I+QgMWviU2dHT0s9pcV88ysKS1y64pD64L1p4zNO9mAUf\n6N1u5+OYIqwCwokN+0JQrA+AyYgTNnooxSvyFzs5nmZKUOMCL4KXwT/qfykzOoQ7\nGwkcIrSJ9mlKwVOtNzsAstxbd9lohQI7mrrMqRr+LNyi488U41Yp6Emlrbh327Y5\nqNDgUMeixk9DFC7l6X3oKSLal37F59wOx0xY3Dyc4YaqT4iT9CGKX1kCignyoYYG\nNDXAnnX+sH+hDf/YKv8D3l3e2oHVsjpYAcdx44dZeQKBgQDYiV/j6AF5+PMGznns\nDXab5UeIAWVRw15kAZ/Tz7ugInsieG6FWJRjmE6LUIwuE8m9EVQM9d5yWRfup48c\nNvzWBCpdf3qcMQ9/184i+OSfspfLzHh3kG4ZODmacuCJbHcEmND7/931sTIpn6C4\nFMPkA/npJmec9WO6PrnANIqQOQKBgQC2ruzcG9VTsi4rYHzhYH5oOwgKUGV7Dzng\nHH+HQuddsc/R12fajDQxkCwZ7xhLBNpMa94unrPVzRpvn7JHPWu9wF140Jvfv2TQ\nByK+ZkZkYE9jwJJG2RogG3icmibX4Dy2SxZAORP2dAzl2VmGB18g29vNx9TWABfJ\nRmIVii+sDwKBgQDCWMIk5sX0eJl/gXByij7reyTxj/clJImijsapsCgBeRE1hgwE\nSNWeckQEzD2U2ZPWyye6Hi8SSJ6vR9qtgzj4yXJQyqr8Zxg3MTVFtEhWgU+2zgVN\noub+YbVpxzN6hExqZtIja6oG64xPSYNR1OlgzKTQOvUCLkjdD5FeTooTGQKBgQCL\nuyDQ+zkg25kU9KiQew41C3IK8ZOtnOfX0/R3sWTm9arQBsMZF0SU5IHXFFqwhvWy\nno9DeYB0eH1QBAaJFyHh8dGNJ4MSkYK627pniDMvc7tFwXYhWs3w4RJQNlifppTJ\nmoVaek4apIl5BdbD/b91krV9oOvbBBctjPhZIWl1YQKBgQCj6I/l4TcqWqagqDTf\nDl2l7djPbBT4O2x2Wm3j59NGyCRwZeHShHZS46faAEGowZYS9W3Iln5iWPfjLnFs\nFeaiqBc4lR74cWWKNX8zpU5MTf7FahJPucDH69248bNDsmL6ePDjPcpKMJP3LUXH\n5ouMHWDX8/EaswvrJP1HFbjtkQ==\n-----END PRIVATE KEY-----\n\",\n" +
                    "  \"client_email\": \"firebase-adminsdk-z1qia@project-showtime-f6e40.iam.gserviceaccount.com\",\n" +
                    "  \"client_id\": \"109037663444565160220\",\n" +
                    "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                    "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                    "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                    "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-z1qia%40project-showtime-f6e40.iam.gserviceaccount.com\"\n" +
                    "}";

            ByteArrayInputStream serviceAccountStream = new ByteArrayInputStream(serviceAccountJson.getBytes());

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                    .setDatabaseUrl("https://project-showtime-f6e40.firebaseio.com")
                    .build();
            FirebaseApp.initializeApp(options);
            firestore = FirestoreClient.getFirestore();
            logger.info("Firebase initialized successfully");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to initialize Firebase: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        logger.info("Command received: " + command.getName());
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

    @Override
    public void onDisable() {
        logger.info("showtimeplugin is being disabled...");
        if (firestore != null) {
            try {
                firestore.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            logger.info("Firestore closed.");
        }
    }

}