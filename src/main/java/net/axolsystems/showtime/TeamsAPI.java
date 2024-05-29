package net.axolsystems.showtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Supplier;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TeamsAPI {

    // Base URL of the Teams API
    private static final String API_BASE_URL = "https://project-showtime.vercel.app";

    // Method to get all team data from the API
    public static JSONObject getAllTeamData() {
        try {
            // Construct the API URL for getting team data
            String apiUrl = API_BASE_URL + "/api/teamdata";
            URL url = new URL(apiUrl);

            // Open a connection to the API endpoint
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Read the response content
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse the response as a JSON object
            return new JSONObject(response.toString());

        } catch (IOException e) {
            System.out.println("Error fetching team data: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            System.out.println("Error parsing team data as JSON object: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    public static boolean addTeam(String teamName, String color, String[] players) {
        try {
            // Construct the API URL for adding a new team
            String apiUrl = API_BASE_URL + "/api/addTeam";
            URL url = new URL(apiUrl);

            // Open a connection to the API endpoint
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Construct the request payload
            JSONObject payload = new JSONObject();
            payload.put("teamName", teamName);
            payload.put("colour", color);
            for (int i = 0; i < players.length; i++) {
                payload.put("player" + (i + 1), players[i]);
                ShowTime.logger.info("player" + (i + 1) + ": " + players[i]);
            }

            // Log the JSON payload
            ShowTime.logger.info("JSON Payload: " + payload.toString());

            // Write the payload to the output stream
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
