package farahsoftware.co.za;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlayerJoinListener {

    private final DatabaseManager database;
    private final ogmcdc plugin;
    private final Scheduler scheduler;

    public PlayerJoinListener(DatabaseManager database, ogmcdc plugin, Scheduler scheduler) {
        this.database = database;
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PluginConfig config = ConfigLoader.getInstance().getConfig();

        scheduler.buildTask(plugin, () -> {
            if (database.isPlayerLinked(uuid)) {
                // Get Discord username
                String discordUsername = database.getDiscordUsername(uuid);
                String linkedMsg = config.linkedMessage.linked + " (" + discordUsername + ")";
                player.sendMessage(Component.text(linkedMsg, NamedTextColor.YELLOW));

                plugin.syncRolesForPlayer(uuid);
            } else {
                String code = database.generateAndStoreVerificationCode(uuid);

                String encodedRedirect = URLEncoder.encode(config.discord.redirectUrl, StandardCharsets.UTF_8);
                String oauthUrl = config.linkMessage.linkUrl +
                        "?client_id=" + config.discord.clientId +
                        "&response_type=code" +
                        "&redirect_uri=" + encodedRedirect +
                        "&scope=identify+guilds.members.read+guilds" +
                        "&state=" + code;

                Component message = Component.text(config.linkMessage.prefix, NamedTextColor.YELLOW)
                        .append(Component.text(config.linkMessage.linkText)
                                .color(NamedTextColor.GREEN)
                                .clickEvent(ClickEvent.openUrl(oauthUrl))
                                .hoverEvent(HoverEvent.showText(Component.text(config.linkMessage.hoverText))))
                        .append(Component.text(config.linkMessage.suffix, NamedTextColor.YELLOW));

                player.sendMessage(message);
            }
        }).delay(5, TimeUnit.SECONDS).schedule();
    }
}
