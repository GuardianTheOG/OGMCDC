package farahsoftware.co.za;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
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
            source.sendMessage(Component.text("Usage: /ogmcdc <link|unlink|reload>", NamedTextColor.YELLOW));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "link":
                handleLinkCommand(source);
                break;
            case "unlink":
                handleUnlinkCommand(source);
                break;
            case "reload":
                handleReloadCommand(source);
                break;
            default:
                source.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
        }
    }

    private void handleLinkCommand(CommandSource source) {
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return;
        }

        UUID uuid = player.getUniqueId();

        if (databaseManager.isPlayerLinked(uuid)) {
            player.sendMessage(Component.text("Your account is already linked to Discord.", NamedTextColor.YELLOW));
            return;
        }

        String code = CodeGenerator.generateCode();
        databaseManager.storeVerificationCode(uuid, code);

        String link = "https://farahsoftware.co.za/mrg?code=" + code;
        player.sendMessage(Component.text("Click here to link your account to Discord: ")
                .append(Component.text(link)
                        .clickEvent(ClickEvent.openUrl(link))
                        .color(NamedTextColor.GREEN)));
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

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 1) {
            return List.of("link", "unlink", "reload");
        }
        return List.of();
    }
}
