package farahsoftware.co.za;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class PluginConfig {

    public Discord discord;
    public LinkCode linkCode;
    public Map<String, String> roles;
    public LinkMessage linkMessage;
    public LinkedMessage linkedMessage;
    public Webhook webhook;
    public MySQL mysql;

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
}
