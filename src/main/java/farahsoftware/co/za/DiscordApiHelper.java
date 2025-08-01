package farahsoftware.co.za;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class DiscordApiHelper {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void syncRoles(
            String botToken,
            String guildId,
            String userId,
            List<String> userRolesFromDb,
            Map<String, String> roleMapFromConfig
    ) throws Exception {
        // Build final role list using config mappings
        List<String> finalRoleIds = new ArrayList<>();
        for (String roleKey : userRolesFromDb) {
            String roleId = roleMapFromConfig.get(roleKey);
            if (roleId != null) {
                finalRoleIds.add(roleId);
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("roles", finalRoleIds);

        String endpoint = "https://discord.com/api/v10/guilds/" + guildId + "/members/" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("PATCH");
        connection.setRequestProperty("Authorization", "Bot " + botToken);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            mapper.writeValue(os, body);
        }

        int code = connection.getResponseCode();
        if (code != 204 && code != 200) {
            throw new RuntimeException("Discord API returned status " + code);
        }
    }
}
