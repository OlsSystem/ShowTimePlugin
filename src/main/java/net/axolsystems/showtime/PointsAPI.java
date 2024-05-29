package net.axolsystems.showtime;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PointsAPI {

    // Base URL of the Points API
    private static final String API_BASE_URL = "https://project-showtime.vercel.app";

    // Method to add points to a player in the API
    public static boolean addPoints(String playerName, int points) {
        try {
            // Construct the API URL for adding points
            String apiUrl = API_BASE_URL + "/api/pointsystem";
            URL url = new URL(apiUrl);

            // Open a connection to the API endpoint
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Construct the request payload
            JSONObject payload = new JSONObject();
            payload.put("username", playerName);
            payload.put("pointAmount", points);
            payload.put("operation", "Add");

            ShowTime.logger.info("JSON Payload: " + payload.toString());

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes();
                os.write(input, 0, input.length);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Check the response code to determine if the operation was successful
            int responseCode = conn.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;

        } catch (Exception e) {
            e.printStackTrace();
            // Handle any errors or exceptions
            return false;
        }
    }

    public static boolean subtractPoints(String playerName, int points) {
        try {
            // Construct the API URL for subtracting points
            String apiUrl = API_BASE_URL + "/api/pointsystem";
            URL url = new URL(apiUrl);

            // Open a connection to the API endpoint
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Construct the request payload
            JSONObject payload = new JSONObject();
            payload.put("username", playerName);
            payload.put("pointAmount", points);
            payload.put("operation", "Remove");

            ShowTime.logger.info("JSON Payload: " + payload.toString());

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes();
                os.write(input, 0, input.length);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Check the response code to determine if the operation was successful
            int responseCode = conn.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;

        } catch (Exception e) {
            e.printStackTrace();
            // Handle any errors or exceptions
            return false;
        }
    }

}
