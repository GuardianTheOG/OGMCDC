package farahsoftware.co.za;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigLoader {

    private final Logger logger;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;

    private boolean isFirstRun = false;

    public ConfigLoader(Logger logger) {
        this.logger = logger;
        loadConfig();
    }

    private void loadConfig() {
        try {
            File pluginFolder = new File("plugins/ogmcdc");
            if (!pluginFolder.exists()) {
                if (pluginFolder.mkdirs()) {
                    logger.info("Created plugin directory: " + pluginFolder.getPath());
                }
            }

            File configFile = new File(pluginFolder, "config.yml");

            if (!configFile.exists()) {
                logger.warn("Config file not found. Creating default config.yml...");

                createDefaultConfig(configFile);
                isFirstRun = true;
                logger.warn("Default config.yml created. Please edit the file with your database settings and restart the server.");
                return;
            }

            InputStream input = new FileInputStream(configFile);
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);

            host = (String) data.getOrDefault("host", "localhost");
            port = (int) data.getOrDefault("port", 3306);
            database = (String) data.getOrDefault("database", "ogmcdc");
            username = (String) data.getOrDefault("username", "user");
            password = (String) data.getOrDefault("password", "pass");

            logger.info("Configuration loaded from config.yml.");

        } catch (Exception e) {
            logger.error("Failed to load config.yml: " + e.getMessage(), e);
        }
    }

    private void createDefaultConfig(File configFile) {
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("""
                host: "localhost"
                port: 3306
                database: "ogmcdc"
                username: "user"
                password: "pass"
                """);
        } catch (IOException e) {
            logger.error("Failed to create default config.yml: " + e.getMessage(), e);
        }
    }

    public boolean isFirstRun() {
        return isFirstRun;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
