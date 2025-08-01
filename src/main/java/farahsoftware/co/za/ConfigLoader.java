package farahsoftware.co.za;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigLoader {

    private static ConfigLoader instance;
    private final PluginConfig config;

    private ConfigLoader(Path dataDirectory, Logger logger) {
        File dataFolder = dataDirectory.toFile();
        File configFile = new File(dataFolder, "config.yml");

        if (!dataFolder.exists()) dataFolder.mkdirs();

        if (!configFile.exists()) {
            logger.warn("⚠ config.yml not found. Creating from internal resources...");
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in == null) {
                    throw new IOException("Default config.yml not found in resources!");
                }
                Files.copy(in, configFile.toPath());
                logger.info("✅ Default config.yml created.");
            } catch (IOException e) {
                throw new RuntimeException("❌ Failed to create config.yml", e);
            }
        }

        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            this.config = mapper.readValue(configFile, PluginConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("❌ Failed to load config.yml", e);
        }
    }

    public static void init(Path dataDirectory, Logger logger) {
        if (instance == null) {
            instance = new ConfigLoader(dataDirectory, logger);
        }
    }

    public static void reload(Path dataDirectory, Logger logger) {
        instance = new ConfigLoader(dataDirectory, logger);
    }

    public static ConfigLoader getInstance() {
        if (instance == null) throw new IllegalStateException("Call ConfigLoader.init(...) before using getInstance()");
        return instance;
    }

    public PluginConfig getConfig() {
        return config;
    }
}
