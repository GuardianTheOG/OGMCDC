package farahsoftware.co.za;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Command implements SimpleCommand {

    private final DatabaseManager databaseManager;
    private final ogmcdc plugin;

    public Command(DatabaseManager databaseManager, ogmcdc plugin) {
        this.databaseManager = databaseManager;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            source.sendMessage(Component.text("Usage: /ogmcdc <link|unlink|reload|status>", NamedTextColor.YELLOW));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "link" -> handleLinkCommand(source);
            case "unlink" -> handleUnlinkCommand(source);
            case "reload" -> handleReloadCommand(source);
            case "status" -> handleStatusCommand(source);
            default -> source.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
        }
    }

    private void handleLinkCommand(CommandSource source) {
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return;
        }

        UUID uuid = player.getUniqueId();
        PluginConfig config = ConfigLoader.getInstance().getConfig();

        if (databaseManager.isPlayerLinked(uuid)) {
            String discordUsername = databaseManager.getDiscordUsername(uuid);
            String linkedMsg = config.linkedMessage.linked + " (" + discordUsername + ")";
            player.sendMessage(Component.text(linkedMsg, NamedTextColor.YELLOW));
            return;
        }

        String code = CodeGenerator.generateCode();
        databaseManager.storeVerificationCode(uuid, code);

        String redirectUriEncoded = URLEncoder.encode(config.discord.redirectUrl, StandardCharsets.UTF_8);
        String oauthUrl = config.linkMessage.linkUrl +
                "?client_id=" + config.discord.clientId +
                "&response_type=code" +
                "&redirect_uri=" + redirectUriEncoded +
                "&scope=identify+guilds.members.read+guilds" +
                "&state=" + code;

        TextComponent message = Component.text()
                .append(Component.text(config.linkMessage.prefix, NamedTextColor.YELLOW))
                .append(Component.text(config.linkMessage.linkText)
                        .color(NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.openUrl(oauthUrl))
                        .hoverEvent(HoverEvent.showText(Component.text(config.linkMessage.hoverText))))
                .append(Component.text(config.linkMessage.suffix, NamedTextColor.YELLOW))
                .build();

        player.sendMessage(message);
    }

    private void handleUnlinkCommand(CommandSource source) {
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return;
        }

        UUID uuid = player.getUniqueId();

        if (!databaseManager.isPlayerLinked(uuid)) {
            player.sendMessage(Component.text("You are not currently linked to a Discord account.", NamedTextColor.YELLOW));
            return;
        }

        databaseManager.unlinkPlayer(uuid);
        player.sendMessage(Component.text("Your Discord link has been removed.", NamedTextColor.GREEN));
    }

    private void handleReloadCommand(CommandSource source) {
        plugin.reload();
        source.sendMessage(Component.text("OGMCDC plugin reloaded.", NamedTextColor.GREEN));
    }

    private void handleStatusCommand(CommandSource source) {
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return;
        }

        UUID uuid = player.getUniqueId();
        String name = player.getUsername();
        PluginConfig config = ConfigLoader.getInstance().getConfig();

        boolean linked = databaseManager.isPlayerLinked(uuid);
        String discordId = linked ? databaseManager.getDiscordId(uuid) : "N/A";
        String discordName = linked ? databaseManager.getDiscordUsername(uuid) : "N/A";

        String status = linked ? "✅ Linked" : "❌ Unlinked";

        player.sendMessage(Component.text("Status: ", NamedTextColor.GRAY)
                .append(Component.text(status, linked ? NamedTextColor.GREEN : NamedTextColor.RED)));

        player.sendMessage(Component.text("Minecraft Name: ", NamedTextColor.GRAY)
                .append(Component.text(name, NamedTextColor.YELLOW)));

        player.sendMessage(Component.text("Minecraft ID: ", NamedTextColor.GRAY)
                .append(Component.text(uuid.toString(), NamedTextColor.YELLOW)));

        player.sendMessage(Component.text("Discord Name: ", NamedTextColor.GRAY)
                .append(Component.text(discordName, NamedTextColor.AQUA)));

        player.sendMessage(Component.text("Discord ID: ", NamedTextColor.GRAY)
                .append(Component.text(discordId, NamedTextColor.AQUA)));

        if (linked) {
            player.sendMessage(Component.text("Message: Thank you for linking your account!", NamedTextColor.GREEN));
        } else {
            String code = CodeGenerator.generateCode();
            databaseManager.storeVerificationCode(uuid, code);

            String redirectUriEncoded = URLEncoder.encode(config.discord.redirectUrl, StandardCharsets.UTF_8);
            String oauthUrl = config.linkMessage.linkUrl +
                    "?client_id=" + config.discord.clientId +
                    "&response_type=code" +
                    "&redirect_uri=" + redirectUriEncoded +
                    "&scope=identify+guilds.members.read+guilds" +
                    "&state=" + code;

            TextComponent linkMessage = Component.text()
                    .append(Component.text("Click to link your account: ", NamedTextColor.RED))
                    .append(Component.text(config.linkMessage.linkText)
                            .color(NamedTextColor.BLUE)
                            .clickEvent(ClickEvent.openUrl(oauthUrl))
                            .hoverEvent(HoverEvent.showText(Component.text(config.linkMessage.hoverText))))
                    .build();

            player.sendMessage(linkMessage);
        }
    }


    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 1) {
            return List.of("link", "unlink", "reload", "status");
        }
        return List.of();
    }
}
