package farahsoftware.co.za;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final HikariDataSource dataSource;

    public DatabaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        PluginConfig.MySQL mysql = ConfigLoader.getInstance().getConfig().mysql;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + mysql.host + ":" + mysql.port + "/" + mysql.database + "?useSSL=false");
        config.setUsername(mysql.username);
        config.setPassword(mysql.password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(30000);

        this.dataSource = new HikariDataSource(config);
        setupTables();
    }

    private void setupTables() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS linked_accounts (
                    uuid VARCHAR(36) PRIMARY KEY,
                    discord_id VARCHAR(32) NOT NULL,
                    linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS verification_codes (
                    uuid VARCHAR(36) PRIMARY KEY,
                    code VARCHAR(10) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isPlayerLinked(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT discord_id FROM discord_links WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void storeVerificationCode(UUID uuid, String code) {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM verification_codes WHERE uuid = ?")) {
                del.setString(1, uuid.toString());
                del.executeUpdate();
            }

            try (PreparedStatement insert = conn.prepareStatement("INSERT INTO verification_codes (uuid, code) VALUES (?, ?)")) {
                insert.setString(1, uuid.toString());
                insert.setString(2, code);
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Optional<String> getVerificationCode(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT code FROM verification_codes WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("code"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<String> getLinkedDiscordId(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT discord_id FROM discord_links WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("discord_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public void unlinkPlayer(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM discord_links WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String generateAndStoreVerificationCode(UUID uuid) {
        String code = CodeGenerator.generateCode();
        storeVerificationCode(uuid, code);
        return code;
    }

    public void cleanupExpiredCodes(int expiryMinutes) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                 DELETE FROM verification_codes
                 WHERE created_at < (NOW() - INTERVAL ? MINUTE)
             """)) {
            stmt.setInt(1, expiryMinutes);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public String getPlayerRank(UUID uuid) {
        // TODO: Implement rank logic
        return "Alnilam";
    }

    public List<String> getStaffRoles(UUID uuid) {
        // TODO: Implement role lookup logic
        return List.of("Helper");
    }
    public String getDiscordUsername(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT discord_username FROM linked_accounts WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("discord_username");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }
    public String getDiscordId(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT discord_id FROM linked_accounts WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("discord_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public List<String> getRoles(UUID uuid) {
        List<String> roles = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT role FROM user_roles WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                roles.add(rs.getString("role"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return roles;
    }



}
