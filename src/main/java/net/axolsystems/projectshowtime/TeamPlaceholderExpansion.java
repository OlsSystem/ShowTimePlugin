package net.axolsystems.projectshowtime;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;


import java.util.concurrent.ExecutionException;

public class TeamPlaceholderExpansion extends PlaceholderExpansion {

    private final ShowTime plugin;
    private final Firestore firestore;

    public TeamPlaceholderExpansion(ShowTime plugin) {
        this.plugin = plugin;
        this.firestore = FirestoreClient.getFirestore();
    }

    @Override
    public boolean persist() {
        return true; // This means you don't have to register it again after a reload
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

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (identifier.startsWith("points_")) {
            String teamName = identifier.split("_")[1];
            DocumentReference teamRef = firestore.collection("teams").document(teamName);
            try {
                ApiFuture<DocumentSnapshot> future = teamRef.get();
                DocumentSnapshot document = future.get();
                if (document.exists()) {
                    Long points = document.getLong("points");
                    return points != null ? String.valueOf(points) : "0";
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return "0"; // Team not found or error occurred
        }
        return null; // Placeholder not recognized
    }
}
