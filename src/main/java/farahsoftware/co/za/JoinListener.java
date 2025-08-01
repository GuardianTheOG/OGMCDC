package farahsoftware.co.za;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import java.util.UUID;

public class JoinListener {

    private final ProxyServer server;
    private final DatabaseManager databaseManager;

    public JoinListener(ProxyServer server, DatabaseManager databaseManager) {
        this.server = server;
        this.databaseManager = databaseManager;
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (databaseManager.isPlayerLinked(uuid)) {
            return; // Already linked, do nothing
        }

        // Generate and store code
        String code = CodeGenerator.generateCode();
        databaseManager.storeVerificationCode(uuid, code);

        // Construct link
        String link = "https://farahsoftware.co.za/mrg?code=" + code;

        // Send clickable message
        player.sendMessage(Component.text("Click here to link your account to Discord: ")
                .append(Component.text(link)
                        .clickEvent(ClickEvent.openUrl(link))
                        .color(net.kyori.adventure.text.format.NamedTextColor.GREEN))
        );
    }
}
