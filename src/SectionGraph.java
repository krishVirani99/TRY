import java.util.*;

public final class SectionGraph {

    public static final Set<Integer> PASSENGER_SECTIONS =
            new HashSet<>(Arrays.asList(1,2,4,5,6,8,9,10));
    public static final Set<Integer> FREIGHT_SECTIONS =
            new HashSet<>(Arrays.asList(3,7,11));

    public static final Set<Integer> SOUTH_ENTRIES =
            new HashSet<>(Arrays.asList(1,3));
    public static final Set<Integer> NORTH_ENTRIES =
            new HashSet<>(Arrays.asList(4,9,10,11));

    private static final Map<Integer, List<Integer>> P = new HashMap<>();
    private static final Map<Integer, List<Integer>> F = new HashMap<>();

    static {
        // Passenger
        link(P, 1, 5);
        link(P, 5, 6);
        link(P, 6, 2);
        link(P, 5, 4);
        link(P, 5, 8);
        link(P, 6, 9);
        link(P, 9, 10);
        link(P, 10, 2);

        // Freight
        link(F, 3, 7);
        link(F, 7, 11);
    }

    private static void link(Map<Integer, List<Integer>> g, int a, int b) {
        g.computeIfAbsent(a, k -> new ArrayList<>()).add(b);
        g.computeIfAbsent(b, k -> new ArrayList<>()).add(a);
    }

    public static List<Integer> neighbors(int section, boolean freight) {
        Map<Integer, List<Integer>> g = freight ? F : P;
        List<Integer> out = g.get(section);
        return out == null ? java.util.Collections.emptyList() : out;
    }

    public static boolean isPassengerSection(int s) { return PASSENGER_SECTIONS.contains(s); }
    public static boolean isFreightSection(int s)   { return FREIGHT_SECTIONS.contains(s);  }

    private SectionGraph() {}
}
