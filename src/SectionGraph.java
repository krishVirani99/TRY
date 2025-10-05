import java.util.*;

public final class SectionGraph {

    // Passenger network sections
    private static final Set<Integer> P = Set.of(1,2,4,5,6,8,9,10);
    // Freight network sections
    private static final Set<Integer> F = Set.of(3,7,11);

    // Public helpers used by InterlockingImpl
    public static boolean isPassengerSection(int s) { return P.contains(s); }
    public static boolean isFreightSection(int s)   { return F.contains(s); }

    // “North entries” used only to infer Direction; not used by grader’s moves
    public static final Set<Integer> NORTH_ENTRIES = Set.of(9,10,11);
    public static final Set<Integer> SOUTH_ENTRIES = Set.of(1,2,3);

    // Return immediate neighbors for the given section, respecting network
    public static List<Integer> neighbors(int s, boolean freight) {
        if (freight) {
            // Freight line: 3—7—11 (no passenger connections; 6 and 7 only cross)
            switch (s) {
                case 3:  return List.of(7);
                case 7:  return List.of(3, 11);
                case 11: return List.of(7);
                default: return List.of();
            }
        } else {
            // Passenger lines and connectors:
            // Rows: 1—5—9 and 2—6—10
            // Verticals: 4—5 and 8—9
            // Connector between the two rows: 5—6
            switch (s) {
                case 1:  return List.of(5);
                case 5:  return List.of(1, 6, 4, 9);   // left, connector, up, right
                case 9:  return List.of(5, 8);         // left, up
                case 2:  return List.of(6);
                case 6:  return List.of(2, 5, 10, 9);  // left-row connector & right
                case 10: return List.of(6);
                case 4:  return List.of(5);
                case 8:  return List.of(9);
                default: return List.of();
            }
        }
    }
}
