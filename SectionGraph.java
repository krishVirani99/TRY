import java.util.*;

/**
 * Static topology of the (simplified) Islington corridor.
 * Passenger and freight lines are disjoint; crossing is handled in InterlockingImpl.
 */
public final class SectionGraph {

    // Passenger sections (blue) and Freight sections (orange)
    public static final Set<Integer> PASSENGER_SECTIONS =
            new HashSet<>(Arrays.asList(1,2,4,5,6,8,9,10));
    public static final Set<Integer> FREIGHT_SECTIONS =
            new HashSet<>(Arrays.asList(3,7,11));

    // Entry sections for direction inference
    public static final Set<Integer> SOUTH_ENTRIES =
            new HashSet<>(Arrays.asList(1,3));
    public static final Set<Integer> NORTH_ENTRIES =
            new HashSet<>(Arrays.asList(4,9,10,11));

    // Undirected adjacency for each line (movement direction decided by InterlockingImpl)
    private static final Map<Integer, List<Integer>> P = new HashMap<>();
    private static final Map<Integer, List<Integer>> F = new HashMap<>();

    static {
        // ---- Passenger line
        // spine: 1-5-6-2
        link(P, 1, 5);
        link(P, 5, 6);
        link(P, 6, 2);
        // branches/exits
        link(P, 5, 4);   // north entry 4
        link(P, 5, 8);   // south exit 8
        link(P, 6, 9);   // crossing connection (passenger side) + south exit 9
        link(P, 9, 10);  // 9-10-2 gives north entries 9/10 path to 2
        link(P, 10, 2);

        // ---- Freight line
        link(F, 3, 7);
        link(F, 7, 11);  // 11 also used as a north entry
    }

    private static void link(Map<Integer, List<Integer>> g, int a, int b) {
        g.computeIfAbsent(a, k -> new ArrayList<>()).add(b);
        g.computeIfAbsent(b, k -> new ArrayList<>()).add(a);
    }

    /** Adjacent neighbors on the appropriate line. */
    public static List<Integer> neighbors(int section, boolean freight) {
        Map<Integer, List<Integer>> g = freight ? F : P;
        List<Integer> out = g.get(section);
        return out == null ? Collections.emptyList() : out;
    }

    public static boolean isPassengerSection(int s) { return PASSENGER_SECTIONS.contains(s); }
    public static boolean isFreightSection(int s)   { return FREIGHT_SECTIONS.contains(s);  }

    private SectionGraph() {}
}
