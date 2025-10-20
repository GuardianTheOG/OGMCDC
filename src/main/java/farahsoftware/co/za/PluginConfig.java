package farahsoftware.co.za;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PluginConfig {

    public Discord discord;
    public LinkCode linkCode;
    public Map<String, String> roles;
    public LinkMessage linkMessage;
    public LinkedMessage linkedMessage;
    public Webhook webhook;
    public MySQL mysql;
    public Reward reward;

    private transient Map<String, String> roleToRank;
    private transient Map<String, String> rankToRole;

    public void initialize() {
        if (roles != null) {
            Map<String, String> roleToRankTmp = new HashMap<>();
            Map<String, String> rankToRoleTmp = new HashMap<>();
            for (Map.Entry<String, String> entry : roles.entrySet()) {
                String rank = entry.getKey();        // e.g., "mod"
                String roleId = entry.getValue();    // e.g., "1268..."
                rankToRoleTmp.put(rank, roleId);
                roleToRankTmp.put(roleId, rank);
            }
            this.rankToRole = Collections.unmodifiableMap(rankToRoleTmp);
            this.roleToRank = Collections.unmodifiableMap(roleToRankTmp);
        }
    }

    public Map<String, String> getRoleToRank() {
        return roleToRank != null ? roleToRank : Collections.emptyMap();
    }

    public Map<String, String> getRankToRole() {
        return rankToRole != null ? rankToRole : Collections.emptyMap();
    }

    public static class Discord {
        public String token;
        public String guildId;
        public String clientId;
        public String clientSecret;
        public String redirectUrl;
    }

    public static class LinkCode {
        public int expiryInMinutes;
        public int codeLength;
    }

    public static class LinkMessage {
        public String prefix;
        public String linkText;
        public String linkUrl;
        public String hoverText;
        public String suffix;
    }

    public static class LinkedMessage {
        public String linked;
    }

    public static class Webhook {
        @JsonProperty("linkSync")
        public String linkSync;
    }

    public static class MySQL {
        public String host;
        public int port;
        public String database;
        public String username;
        public String password;
    }
    public static class Reward {
        public boolean linkreward;
        public String linkrank;
        public String linktrack;
    }
}
