package farahsoftware.co.za;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebhookSender {

    public static void sendDiscordWebhook(String content) {
        try {
            String webhookUrl = ConfigLoader.getInstance().getConfig().webhook.linkSync;

            if (webhookUrl == null || webhookUrl.isEmpty()) {
                System.err.println("⚠ Webhook URL is missing in config.yml.");
                return;
            }

            URL url = new URL(webhookUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            String jsonPayload = "{\"content\": " + quote(content) + "}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204) {
                System.err.println("⚠ Webhook failed: HTTP " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String quote(String text) {
        return "\"" + text.replace("\"", "\\\"") + "\"";
    }
}
