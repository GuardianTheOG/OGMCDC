package farahsoftware.co.za;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

public class DiscordApiHelper {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Logger logger = LoggerFactory.getLogger(DiscordApiHelper.class);

    // ✅ Push in-game roles TO Discord
    public static void syncRolesToDiscord(
            String botToken,
            String guildId,
            String userId,
            List<String> userRolesFromDb,
            Map<String, String> roleMapFromConfig
    ) throws Exception {
        // Step 1: Get current member info
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/guilds/" + guildId + "/members/" + userId))
                .header("Authorization", "Bot " + botToken)
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        if (getResponse.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch current roles for user: " + getResponse.body());
        }

        JsonNode memberData = mapper.readTree(getResponse.body());
        Set<String> currentRoles = new HashSet<>();
        if (memberData.has("roles")) {
            for (JsonNode roleNode : memberData.get("roles")) {
                currentRoles.add(roleNode.asText());
            }
        }

        // Step 2: Map Minecraft roles to Discord role IDs
        Set<String> desiredRoleIds = new HashSet<>(currentRoles); // start with current roles
        for (String roleKey : userRolesFromDb) {
            String roleId = roleMapFromConfig.get(roleKey);
            if (roleId != null) {
                desiredRoleIds.add(roleId); // append
            } else {
                logger.warn("No Discord role ID mapped for Minecraft rank: {}", roleKey);
            }
        }

        // Step 3: Send PATCH request to update roles
        Map<String, Object> body = new HashMap<>();
        body.put("roles", desiredRoleIds);

        HttpRequest patchRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/guilds/" + guildId + "/members/" + userId))
                .header("Authorization", "Bot " + botToken)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> patchResponse = client.send(patchRequest, HttpResponse.BodyHandlers.ofString());

        if (patchResponse.statusCode() != 204 && patchResponse.statusCode() != 200) {
            throw new RuntimeException("Failed to patch roles: " + patchResponse.statusCode() + " - " + patchResponse.body());
        }
    }


    // 🔄 Pull roles FROM Discord (to determine in-game ranks)
    public static Set<String> fetchRolesFromDiscord(String botToken, String guildId, String userId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/guilds/" + guildId + "/members/" + userId))
                .header("Authorization", "Bot " + botToken)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch Discord roles: " + response.statusCode() + " - " + response.body());
        }

        JsonNode node = mapper.readTree(response.body());
        Set<String> roles = new HashSet<>();
        if (node.has("roles")) {
            for (JsonNode role : node.get("roles")) {
                roles.add(role.asText());
            }
        }
        return roles;
    }

    // ⬅️ Sync Discord roles to Minecraft ranks (using LuckPerms)
    public static void syncDiscordRolesToMinecraft(
            UUID playerUuid,
            String botToken,
            String guildId,
            String discordId,
            Map<String, String> roleToRankMap
    ) throws Exception {
        Set<String> discordRoles = fetchRolesFromDiscord(botToken, guildId, discordId);

        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().loadUser(playerUuid).join();

        // Remove all roles defined in config
        for (String rank : new HashSet<>(roleToRankMap.values())) {
            user.data().remove(InheritanceNode.builder(rank).build());
        }

        // Add roles the user has in Discord
        for (String discordRoleId : discordRoles) {
            String rank = roleToRankMap.get(discordRoleId);
            if (rank != null) {
                user.data().add(InheritanceNode.builder(rank).build());
            }
        }

        luckPerms.getUserManager().saveUser(user);
    }
}
