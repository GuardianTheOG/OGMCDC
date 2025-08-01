package farahsoftware.co.za;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RoleMapper {

    private final Map<String, String> rankToRoleId;

    public RoleMapper() {
        Map<String, String> configMap = ConfigLoader.getInstance().getConfig().roles;
        this.rankToRoleId = configMap != null ? configMap : Collections.emptyMap();
    }

    public String getRoleId(String rank) {
        return rankToRoleId.get(rank.toLowerCase());
    }

    public Map<String, String> getAllRoleMappings() {
        return new HashMap<>(rankToRoleId);
    }
}
