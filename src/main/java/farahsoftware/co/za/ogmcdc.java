package farahsoftware.co.za;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;

import org.slf4j.Logger;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.UUID;

@Plugin(
        id = "ogmcdc",
        name = "OGMCDC",
        version = "1.0",
        description = "Link Minecraft to Discord",
        authors = {"FarahSoftware"}
)
public class ogmcdc {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final PluginContainer pluginContainer;

    private DatabaseManager databaseManager;

    @Inject
    public ogmcdc(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, PluginContainer pluginContainer) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.pluginContainer = pluginContainer;
    }

    @Inject
    public void init(EventManager eventManager) {
        ConfigLoader.init(dataDirectory, logger);
        PluginConfig config = ConfigLoader.getInstance().getConfig();
        this.databaseManager = new DatabaseManager();
        eventManager.register(pluginContainer, new PlayerJoinListener(databaseManager, this, server.getScheduler()));
        server.getCommandManager().register("ogmcdc", new Command(databaseManager, this));
    }

    public void reload() {
        ConfigLoader.reload(dataDirectory, logger);
    }

    public void syncRolesForPlayer(UUID uuid) {
        String userId = databaseManager.getDiscordId(uuid);
        if (userId == null) {
            logger.warn("No Discord ID found for player UUID: " + uuid);
            return;
        }

        PluginConfig config = ConfigLoader.getInstance().getConfig();

        try {
            DiscordApiHelper.syncRoles(
                    config.discord.token,
                    config.discord.guildId,
                    userId,
                    databaseManager.getRoles(uuid),
                    config.roles
            );
            logger.info("✅ Synced roles for UUID: " + uuid);
        } catch (Exception e) {
            logger.error("❌ Failed to sync roles for UUID: " + uuid + " - " + e.getMessage(), e);
        }
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("  /$$$$$$   /$$$$$$        /$$      /$$  /$$$$$$    /$$     /$$    /$$$$$$$   /$$$$$$ ");
        logger.info(" /$$__  $$ /$$__  $$      | $$$    /$$$ /$$__  $$  /$$/    |  $$  | $$__  $$ /$$__  $$");
        logger.info("| $$  \\ $$| $$  \\__/      | $$$$  /$$$$| $$  \\__/ /$$/      \\  $$ | $$  \\ $$| $$  \\__/");
        logger.info("| $$  | $$| $$ /$$$$      | $$ $$/$$ $$| $$      /$$/ /$$$$$$\\  $$| $$  | $$| $$      ");
        logger.info("| $$  | $$| $$|_  $$      | $$  $$$| $$| $$     |  $$|______/ /$$/| $$  | $$| $$      ");
        logger.info("| $$  | $$| $$  \\ $$      | $$\\  $ | $$| $$    $$\\  $$       /$$/ | $$  | $$| $$    $$");
        logger.info("|  $$$$$$/|  $$$$$$/      | $$ \\/  | $$|  $$$$$$/ \\  $$     /$$/  | $$$$$$$/|  $$$$$$/");
        logger.info(" \\______/  \\______/       |__/     |__/ \\______/   \\__/    |__/   |_______/  \\______/ ");
        logger.info("                                                                                      ");
        logger.info("                                                                                      ");
        logger.info("        /$$$$$$   /$$                           /$$                     /$$           ");
        logger.info("       /$$__  $$ | $$                          | $$                    | $$           ");
        logger.info("      | $$  \\__//$$$$$$    /$$$$$$   /$$$$$$  /$$$$$$    /$$$$$$   /$$$$$$$           ");
        logger.info("      |  $$$$$$|_  $$_/   |____  $$ /$$__  $$|_  $$_/   /$$__  $$ /$$__  $$           ");
        logger.info("       \\____  $$ | $$      /$$$$$$$| $$  \\__/  | $$    | $$$$$$$$| $$  | $$           ");
        logger.info("       /$$  \\ $$ | $$ /$$ /$$__  $$| $$        | $$ /$$| $$_____/| $$  | $$           ");
        logger.info("      |  $$$$$$/ |  $$$$/|  $$$$$$$| $$        |  $$$$/|  $$$$$$$|  $$$$$$$           ");
        logger.info("       \\______/   \\___/   \\_______/|__/         \\___/   \\_______/ \\_______/           ");
        logger.info("");
        logger.info("✅ OGMCDC plugin has started!");

    }


}
