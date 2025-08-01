package farahsoftware.co.za;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlayerJoinListener {

    private final DatabaseManager db;

    public PlayerJoinListener(DatabaseManager db) {
        this.db = db;
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Optional<String> discordIdOpt = db.getDiscordId(uuid);

        if (discordIdOpt.isPresent()) {
            // Player is linked — sync roles
            String discordId = discordIdOpt.get();
            String rank = db.getPlayerRank(uuid); // Stub — replace with real rank fetch
            List<String> staffRoles = db.getStaffRoles(uuid); // Stub — same here
            List<String> discordRoles = RoleMapper.getDiscordRoles(rank, staffRoles);

            WebhookSender.sendRoleSync(discordId, discordRoles);
        } else {
            // Not linked — generate code and send link
            String code = db.generateAndStoreVerificationCode(uuid);

            Component message = Component.text("Click here to link your Discord account.")
                    .clickEvent(ClickEvent.openUrl("https://farahsoftware.co.za/mrg?code=" + code));

            player.sendMessage(message);
        }
    }
}
