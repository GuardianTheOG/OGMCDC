package farahsoftware.co.za;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(
        id = "ogmcdc",
        name = "OGMCDC",
        version = "1.0.0",
        description = "Guardian's Discord <-> Minecraft sync plugin",
        url = "https://farahsoftware.co.za",
        authors = {"Rizwan Vasavadvala"}
)
public class ogmcdc {

    private final ProxyServer server;
    private final Logger logger;
    private DatabaseManager databaseManager;
    private ConfigLoader config;

    @Inject
    public ogmcdc(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("OGMCDC plugin is initializing...");

        // Load config
        ConfigLoader configLoader = new ConfigLoader(logger);

        if (configLoader.isFirstRun()) {
            logger.warn("Plugin disabled: Please configure the database settings in config.yml and restart the server.");
            return; // Don't continue plugin startup
        }


        // Initialize DatabaseManager
        this.databaseManager = new DatabaseManager(
                configLoader.getHost(),
                configLoader.getPort(),
                configLoader.getDatabase(),
                configLoader.getUsername(),
                configLoader.getPassword()
        );

        // Register command
        CommandManager commandManager = server.getCommandManager();
        commandManager.register("ogmcdc", new Command(databaseManager, this), "ogmcdc");

        logger.info("OGMCDC plugin initialized successfully.");
    }

    public void reload() {
        logger.info("Reloading OGMCDC plugin...");

        // Reload config
        this.config = new ConfigLoader(logger);
        if (config.isFirstRun()) {
            logger.warn("Config is incomplete. Plugin will not reconnect to the database.");
            return;
        }

        // Rebuild database connection
        this.databaseManager = new DatabaseManager(
                config.getHost(),
                config.getPort(),
                config.getDatabase(),
                config.getUsername(),
                config.getPassword()
        );

        logger.info("Reload complete.");
    }
}
