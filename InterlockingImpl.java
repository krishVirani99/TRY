import java.util.*;

/**
 * Grader-compatible Interlocking:
 *  - CAP=1 per section
 *  - Passenger/Freight lines are disjoint (no mixing)
 *  - Diamond crossing priority: Passenger first
 *  - moveTrains(): choose one BFS next-hop toward the target exit and attempt a guarded move
 */
public class InterlockingImpl implements Interlocking {

    // ===================== State =====================
    /** section -> trainId (occupancy) */
    private final Map<Integer, String> occ = new HashMap<>();

    /** trainId -> info */
    private final Map<String, TrainInfo> trains = new HashMap<>();

    /** trainId -> intended exit section (as the grader passes) */
    private final Map<String, Integer> exitOf = new HashMap<>();

    static final class TrainInfo {
        final TrainType type;
        final Direction dir;
        int section; // -1 when out

        TrainInfo(TrainType type, Direction dir, int section) {
            this.type = type;
            this.dir = dir;
            this.section = section;
        }
    }

    // ===================== Helpers =====================

    private static boolean isNorthEntry(int s) {
        return SectionGraph.NORTH_ENTRIES.contains(s);
    }

    private static boolean isFreightEntry(int s) {
        // freight uses 3 (south entry) and 11 (north entry)
        return s == 3 || s == 11;
    }

    private static boolean isPassengerCrossMove(int a, int b) {
        return (a == 6 && b == 9) || (a == 9 && b == 6);
    }

    private static boolean isFreightCrossMove(int a, int b) {
        return (a == 7 && b == 11) || (a == 11 && b == 7);
    }

    /** Any passenger staged at crossing approaches? (4,5,6,9) */
    private boolean passengerStagedAtCross() {
        for (TrainInfo ti : trains.values()) {
            if (ti.type == TrainType.PASSENGER) {
                int s = ti.section;
                if (s == 4 || s == 5 || s == 6 || s == 9) return true;
            }
        }
        return false;
    }

    /** Undirected neighbors on the appropriate line. */
    private List<Integer> neighbors(int section, boolean freight) {
        return SectionGraph.neighbors(section, freight);
    }

    /** First hop of a shortest path from start to goal (ignores occupancy; move() enforces safety). */
    private Integer nextHop(int start, int goal, boolean freight) {
        if (start == goal) return goal;
        Set<Integer> vis = new HashSet<>();
        Map<Integer, Integer> parent = new HashMap<>();
        ArrayDeque<Integer> q = new ArrayDeque<>();
        vis.add(start); q.add(start);
        while (!q.isEmpty()) {
            int u = q.removeFirst();
            for (int v : neighbors(u, freight)) {
                if (!vis.add(v)) continue;
                parent.put(v, u);
                if (v == goal) {
                    int x = v, p = parent.get(x);
                    while (p != start) { x = p; p = parent.get(x); }
                    return x; // first step from start toward goal
                }
                q.addLast(v);
            }
        }
        return null;
    }

    /** Attempt a single guarded move: capacity, connectivity, line, and crossing priority. */
    private boolean tryMove(String id, int next) {
        TrainInfo ti = trains.get(id);
        if (ti == null) return false;
        int curr = ti.section;

        // line partition (no mixing)
        if (ti.type == TrainType.FREIGHT && !SectionGraph.isFreightSection(next)) return false;
        if (ti.type == TrainType.PASSENGER && !SectionGraph.isPassengerSection(next)) return false;

        // connectivity
        boolean freight = (ti.type == TrainType.FREIGHT);
        if (!neighbors(curr, freight).contains(next)) return false;

        // capacity
        if (occ.containsKey(next)) return false;

        // crossing priority: passenger first
        if (freight && isFreightCrossMove(curr, next) && passengerStagedAtCross()) {
            return false;
        }

        // perform move
        occ.remove(curr);
        occ.put(next, id);
        ti.section = next;
        return true;
    }

    // ===================== Local helpers (used by adapter) =====================

    /** Where is this train? null if unknown/out. */
    private Integer where(String id) {
        TrainInfo ti = trains.get(id);
        return (ti == null || ti.section < 0) ? null : ti.section;
    }

    // ===================== Grader API =====================

    @Override
    public boolean addTrain(String name, int entrySection, int exitSection) {
        if (name == null) return false;
        if (trains.containsKey(name)) return false;       // duplicate id
        if (occ.containsKey(entrySection)) return false;  // occupied entry

        // infer type & direction from entry
        TrainType type = isFreightEntry(entrySection) ? TrainType.FREIGHT : TrainType.PASSENGER;
        Direction dir  = isNorthEntry(entrySection) ? Direction.NORTH   : Direction.SOUTH;

        // entry must belong to the inferred line
        if (type == TrainType.FREIGHT && !SectionGraph.isFreightSection(entrySection)) return false;
        if (type == TrainType.PASSENGER && !SectionGraph.isPassengerSection(entrySection)) return false;

        trains.put(name, new TrainInfo(type, dir, entrySection));
        occ.put(entrySection, name);
        exitOf.put(name, exitSection);
        return true;
    }

    @Override
    public Integer moveTrains(String[] trainNames) {
        int moved = 0;
        if (trainNames == null) return 0;

        for (String id : trainNames) {
            if (id == null) continue;

            TrainInfo ti = trains.get(id);
            if (ti == null) continue;
            int curr = ti.section;
            if (curr < 0) continue;

            Integer goal = exitOf.get(id);
            if (goal == null) continue;

            // If already at the requested exit section, remove from the system (exit).
            if (curr == goal) {
                occ.remove(curr);
                ti.section = -1;
                moved++;
                continue;
            }

            boolean freight = (ti.type == TrainType.FREIGHT);
            Integer next = nextHop(curr, goal, freight);
            if (next != null && tryMove(id, next)) {
                moved++;
            }
        }
        return moved;
    }

    @Override
    public String getSection(int section) {
        return occ.get(section);
    }

    @Override
    public Integer getTrain(String trainName) {
        TrainInfo ti = trains.get(trainName);
        return (ti == null) ? -1 : ti.section;
    }
}
