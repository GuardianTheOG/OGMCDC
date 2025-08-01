package farahsoftware.co.za;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WebhookSender {

    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1400558062594691072/4kuG6W6pF4S2bmFIB87K895kCmtFO1sMyrmm_-7W5E0ZxDdBssz72htes5VHEO8LjqTP";

    public static void sendRoleSync(String discordId, List<String> roleIds) {
        try {
            URL url = new URL(WEBHOOK_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            JsonObject payload = new JsonObject();
            payload.addProperty("discordId", discordId);

            JsonArray rolesArray = new JsonArray();
            for (String roleId : roleIds) {
                rolesArray.add(roleId);
            }
            payload.add("roles", rolesArray);

            String json = payload.toString();

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200 && responseCode != 204) {
                System.out.println("Failed to send role sync webhook: " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
