package farahsoftware.co.za;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.jetbrains.annotations.NotNull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Command implements SimpleCommand {

    private final DatabaseManager databaseManager;
    private final ogmcdc plugin;
    private final ProxyServer proxy;
    private final LuckPerms luckPerms;
    private static ConfigLoader config;

    public Command(DatabaseManager databaseManager, ogmcdc plugin, ProxyServer proxy) {
        this.databaseManager = databaseManager;
        this.plugin = plugin;
        this.proxy = proxy;
        PluginConfig config = ConfigLoader.getInstance().getConfig();
        this.luckPerms = LuckPermsProvider.get();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            source.sendMessage(Component.text("Usage: /ogmcdc <link|unlink|reload|status|sync>", NamedTextColor.YELLOW));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "link" -> handleLinkCommand(source);
            case "unlink" -> handleUnlinkCommand(source);
            case "reload" -> handleReloadCommand(source);
            case "status" -> handleStatusCommand(source, args);
            case "sync" -> handleSyncCommand(source);
            case "help" -> handleHelpCommand(source);
            default -> source.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
        }
    }

    public void handleLinkCommand(CommandSource source) {
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return;
        }

        UUID uuid = player.getUniqueId();
        PluginConfig config = ConfigLoader.getInstance().getConfig();

        if (databaseManager.isPlayerLinked(uuid)) {
            String discordUsername = databaseManager.getDiscordUsername(uuid);
            User user = luckPerms.getUserManager().loadUser(uuid).join();
            String rank = user.getPrimaryGroup();
            String linkedMsg = config.linkedMessage.linked + " (" + discordUsername + ")";
            player.sendMessage(Component.text(linkedMsg, NamedTextColor.YELLOW));
            if (luckPerms !=null) {
                luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(User -> {
                    user.data().add(luckPerms.getNodeBuilderRegistry().forInheritance().group(config.reward.linkrank).build());
                    luckPerms.getUserManager().saveUser(user);
                    plugin.syncRolesForPlayer(uuid);
                    try {
                        DiscordApiHelper.syncRolesToDiscord(config.discord.token, config.discord.guildId, databaseManager.getDiscordId(uuid), databaseManager.getRoles(uuid), config.getRankToRole());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    databaseManager.storeUserRole(uuid, rank);
                });
            }
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

    private void handleStatusCommand(CommandSource source, String[] args) {
        if (args.length == 1) {
            if (!(source instanceof Player player)) {
                source.sendMessage(Component.text("Only players can use this command without a target.", NamedTextColor.RED));
                return;
            }
            showStatus(source, player.getUsername(), player.getUniqueId());
        } else {
            if (!source.hasPermission("ogmcdc.admin")) {
                source.sendMessage(Component.text("You do not have permission to view other players' status.", NamedTextColor.RED));
                return;
            }

            Optional<Player> target = proxy.getPlayer(args[1]);
            if (target.isEmpty()) {
                source.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                return;
            }

            showStatus(source, target.get().getUsername(), target.get().getUniqueId());
        }
    }

    private void showStatus(CommandSource source, String name, UUID uuid) {
        PluginConfig config = ConfigLoader.getInstance().getConfig();
        boolean linked = databaseManager.isPlayerLinked(uuid);
        String discordId = linked ? databaseManager.getDiscordId(uuid) : "N/A";
        String discordName = linked ? databaseManager.getDiscordUsername(uuid) : "N/A";
        String status = linked ? "✅ Linked" : "❌ Unlinked";

        source.sendMessage(Component.text("Status: ", NamedTextColor.WHITE)
                .append(Component.text(status, linked ? NamedTextColor.DARK_GREEN : NamedTextColor.DARK_RED)));

        source.sendMessage(Component.text("Minecraft Name: ", NamedTextColor.WHITE)
                .append(Component.text(name, NamedTextColor.GOLD)));

        source.sendMessage(Component.text("Minecraft ID: ", NamedTextColor.WHITE)
                .append(Component.text(uuid.toString(), NamedTextColor.GOLD)));

        source.sendMessage(Component.text("Discord Name: ", NamedTextColor.WHITE)
                .append(Component.text(discordName, NamedTextColor.GOLD)));

        source.sendMessage(Component.text("Discord ID: ", NamedTextColor.WHITE)
                .append(Component.text(discordId, NamedTextColor.GOLD)));


        if (linked) {
            source.sendMessage(Component.text("Message: Thank you for linking your account!", NamedTextColor.GREEN));
        } else if (source instanceof Player player) {
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

    private void handleHelpCommand(@NotNull CommandSource source) {
        source.sendMessage(Component.text("Usage: /ogmcdc + ", NamedTextColor.GREEN));
        source.sendMessage(Component.text("help - Shows this help message.", NamedTextColor.GREEN));
        source.sendMessage(Component.text("link - Generates a lickable link to link Minecraft account with Discord Account.", NamedTextColor.GREEN));
        source.sendMessage(Component.text("unlink - Unlinks Discord from Minecraft for the server.", NamedTextColor.GREEN));
        source.sendMessage(Component.text("status - Shows your link status.", NamedTextColor.GREEN));
        if (source.hasPermission("ogmcdc.admin")) {
            source.sendMessage(Component.text("status + Player_Name - Shows link status for online player.", NamedTextColor.DARK_GREEN));
            source.sendMessage(Component.text("sync - Force synchronisation of roles.", NamedTextColor.DARK_GREEN));
            source.sendMessage(Component.text("reload - Reloads config file with changes.", NamedTextColor.DARK_GREEN));

        }

    }

    private void handleSyncCommand(CommandSource source) {
        if (!source.hasPermission("ogmcdc.admin")) {
            source.sendMessage(Component.text("You do not have permission to run this command.", NamedTextColor.RED));
            return;
        }
        DatabaseManager database = this.databaseManager;

        for (Player player : proxy.getAllPlayers()) {
            UUID uuid = player.getUniqueId();
            PluginConfig config = ConfigLoader.getInstance().getConfig();
            String discordId = database.getDiscordId(uuid);

            User user = luckPerms.getUserManager().loadUser(uuid).join();
            String rank = user.getPrimaryGroup();
            if (database.isPlayerLinked(uuid)) {
                String discordUsername = database.getDiscordUsername(uuid);
                String linkedMsg = config.linkedMessage.linked + " (" + discordUsername + ")";
                player.sendMessage(Component.text(linkedMsg, NamedTextColor.YELLOW));
                plugin.syncRolesForPlayer(uuid);
                database.storeUserRole(uuid, rank);
                if (discordId != null) {
                    List<String> roles = database.getRoles(uuid);
                    if (!roles.contains("saiph") || roles.isEmpty() && config.reward.linkreward) {
                        String rewardRank = config.reward.linkrank;
                        if (luckPerms != null) {
                            luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(user1 -> {
                            user.data().add(luckPerms.getNodeBuilderRegistry().forInheritance().group(rewardRank).build());
                            luckPerms.getUserManager().saveUser(user);
                            database.storeUserRole(uuid, rewardRank);
                            plugin.syncRolesForPlayer(uuid);
                            });
                        }
                    }
                }
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
        }
        source.sendMessage(Component.text("All linked players have been synced.", NamedTextColor.GREEN));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 1) {
            return List.of("link", "unlink", "reload", "status", "sync", "help").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("status")) {
            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
