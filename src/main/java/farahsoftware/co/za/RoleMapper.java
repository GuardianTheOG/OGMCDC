package farahsoftware.co.za;

import java.util.*;

public class RoleMapper {

    private static final Map<String, String> rankRoles = new LinkedHashMap<>();
    private static final Map<String, String> staffRoles = new HashMap<>();

    static {
        // Compound Rank Progression
        rankRoles.put("Saiph", "1268615790001918062");
        rankRoles.put("Alnitak", "1261674358682943489");
        rankRoles.put("Alnilam", "1261674432670334976");
        rankRoles.put("Mintaka", "1261674583778394184");
        rankRoles.put("Hatysa", "1261674653823533126");
        rankRoles.put("Meissa", "1261674761390657628");
        rankRoles.put("Latrix", "1261674821943820338");
        rankRoles.put("Rigel", "1268616511346446357");
        rankRoles.put("Betel", "1261675074797305906");
        rankRoles.put("Orion", "1261675190287466496");

        // Staff Roles
        staffRoles.put("Default", "1268615790001918062"); // Assuming same as Saiph
        staffRoles.put("Helper", "1268616681194782822");
        staffRoles.put("Mod", "1268616820156399666");
        staffRoles.put("Admin", "1268617040638509117");
    }

    public static List<String> getDiscordRoles(String rank, List<String> staff) {
        List<String> result = new ArrayList<>();

        boolean found = false;
        for (Map.Entry<String, String> entry : rankRoles.entrySet()) {
            result.add(entry.getValue());
            if (entry.getKey().equalsIgnoreCase(rank)) {
                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println("Unknown rank: " + rank);
        }

        for (String staffRole : staff) {
            String roleId = staffRoles.getOrDefault(staffRole, null);
            if (roleId != null) {
                result.add(roleId);
            } else {
                System.out.println("Unknown staff role: " + staffRole);
            }
        }

        return result;
    }
}
