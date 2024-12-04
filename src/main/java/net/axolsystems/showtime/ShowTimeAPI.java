package net.axolsystems.showtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

public class ShowTimeAPI {
    private static final String API_BASE_URL = "https://project-showtime.vercel.app";

    public static boolean apiPostMethod(String endPoint, JSONObject dataToSend) {
        try {
            String apiUrl = API_BASE_URL + endPoint;
            URL url = new URL(apiUrl);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("authorisation", "AUTHORISATIONCODEHERE");
            conn.setDoOutput(true);

            ShowTime.logger.info("JSON Payload: " + dataToSend.toString());

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = dataToSend.toString().getBytes();
                os.write(input, 0, input.length);
            } catch (Exception e) {
                e.printStackTrace();
            }

            int responseCode = conn.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;

        } catch (Exception e) {
            e.printStackTrace();
            ShowTime.logger.warning("Error using post method: " + endPoint + " Error Message: " + e.getMessage());
            return false;
        }
    }

    public static JSONObject apiGetMethod(String endPoint) {
        try {
            String apiUrl = API_BASE_URL + endPoint;
            URL url = new URL(apiUrl);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            StringBuilder response;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            return new JSONObject(response.toString());
        } catch (IOException e) {
            ShowTime.logger.warning("Error using get method: " + endPoint + " Error Message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
