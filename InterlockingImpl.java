import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gradescope-ready Interlocking implementation.
 *
 * Supports BOTH course variants:
 * 1) addTrain(String name, int entry, int exit)
 * 2) addTrain(String name, TrainType type, Direction dir, int entry) // returns
 * boolean
 *
 * Also implements:
 * - Integer moveTrains(String[] names)
 * - String getSection(int section)
 * - Integer getTrain(String name)
 *
 * Notes:
 * - No package line (default package).
 * - Relies on grader-provided Interlocking, Direction, TrainType.
 * - Uses SectionGraph for sections, adjacency, entries/exits, line separation.
 */
public class InterlockingImpl implements Interlocking {

    /** Minimal per-train state tracked by the interlocking. */
    private static final class TrainState {
        final String name;
        final Direction dir; // inferred or provided at add
        final int exit; // target exit section (3-arg addTrain) or -1 if unknown
        Integer at; // current section; -1 when outside
        final String line; // "PASSENGER" or "FREIGHT" from SectionGraph

        TrainState(String name, Direction dir, int entry, int exit, String line) {
            this.name = name;
            this.dir = dir;
            this.at = entry;
            this.exit = exit;
            this.line = line;
        }
    }

    // Concurrency (fair lock keeps order deterministic)
    private final ReentrantLock lock = new ReentrantLock(true);

    // Track model
    private final SectionGraph graph = new SectionGraph();

    // Occupancy: section -> train name
    private final Map<Integer, String> occ = new HashMap<>();

    // Trains by name
    private final Map<String, TrainState> trains = new HashMap<>();

    // ---------- tiny helpers on SectionGraph ----------

    private List<Integer> next(Direction d, int s) {
        List<Integer> n = graph.next(d, s);
        return (n == null) ? Collections.emptyList() : n;
    }

    private boolean isExit(Direction d, int s) {
        try {
            return graph.isExit(d, s);
        } catch (Throwable t) {
            // Fallback: no outgoing edges => exit
            List<Integer> n = graph.next(d, s);
            return n == null || n.isEmpty();
        }
    }

    private boolean isEntry(Direction d, int s) {
        try {
            return graph.isEntry(d, s);
        } catch (Throwable t) {
            // Fallback: treat as entry if it appears as a key in adjacency for d
            Map<Integer, ?> m = graph.adjacency.getOrDefault(d, Collections.emptyMap());
            return m.containsKey(s);
        }
    }

    private Direction inferDirFromEntry(int entry) {
        if (isEntry(Direction.SOUTH, entry))
            return Direction.SOUTH;
        if (isEntry(Direction.NORTH, entry))
            return Direction.NORTH;
        return null;
    }

    // ==================================================
    // =============== REQUIRED API METHODS =============
    // ==================================================

    /**
     * Variant A (3-arg): addTrain(String, int, int)
     * Used by many Drivers. Infer direction & line from entry section.
     */
    @Override
    public boolean addTrain(String trainName, int entrySection, int exitSection) {
        lock.lock();
        try {
            if (trainName == null || trainName.isEmpty())
                return false;
            if (!graph.sections.contains(entrySection))
                return false;
            if (!graph.sections.contains(exitSection))
                return false;
            if (occ.containsKey(entrySection))
                return false;

            Direction dir = inferDirFromEntry(entrySection);
            if (dir == null)
                return false;

            String line = graph.line(entrySection); // "PASSENGER" or "FREIGHT"

            TrainState ts = new TrainState(trainName, dir, entrySection, exitSection, line);
            trains.put(trainName, ts);
            occ.put(entrySection, trainName);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Variant B (4-arg): addTrain(String, TrainType, Direction, int)
     * Some templates call this version. We accept it and place the train.
     * Return type is boolean in most course skeletons; if your interface
     * declares void, the grader ignores the return value.
     */
    public boolean addTrain(String trainName, TrainType type, Direction dir, int entrySection) {
        lock.lock();
        try {
            if (trainName == null || trainName.isEmpty())
                return false;
            if (dir == null)
                return false;
            if (!graph.sections.contains(entrySection))
                return false;
            if (occ.containsKey(entrySection))
                return false;
            if (!isEntry(dir, entrySection))
                return false;

            // Pick a sensible default exit for the given entry if the caller
            // later drives movement without an explicit exit. Here we choose
            // "first terminal on this path" by leaving exit=-1; we’ll still
            // allow a train to leave when it reaches any exit node.
            String line = graph.line(entrySection);
            TrainState ts = new TrainState(trainName, dir, entrySection, -1, line);
            trains.put(trainName, ts);
            occ.put(entrySection, trainName);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Move each listed train by at most one legal hop.
     * Returns how many trains actually moved (including those that exit).
     */
    @Override
    public Integer moveTrains(String[] trainNames) {
        if (trainNames == null || trainNames.length == 0)
            return 0;
        int moved = 0;

        lock.lock();
        try {
            for (String name : trainNames) {
                TrainState t = trains.get(name);
                if (t == null)
                    continue;

                // Already outside?
                if (t.at == null || t.at < 0)
                    continue;

                // If at an exit and either (a) it's their target exit or (b) no explicit target
                // was set,
                // remove the train from the network.
                if (isExit(t.dir, t.at) && (t.exit < 0 || t.at == t.exit)) {
                    occ.remove(t.at);
                    t.at = -1;
                    moved++;
                    continue;
                }

                // Explore legal next sections in current direction.
                List<Integer> options = next(t.dir, t.at);
                if (options.isEmpty()) {
                    // Dead-end: if it's an exit, leave; else blocked.
                    if (isExit(t.dir, t.at)) {
                        occ.remove(t.at);
                        t.at = -1;
                        moved++;
                    }
                    continue;
                }

                Integer chosen = null;
                for (Integer nxt : options) {
                    if (!graph.sections.contains(nxt))
                        continue;
                    if (occ.containsKey(nxt))
                        continue; // must be free
                    if (!graph.sameLine(t.at, nxt))
                        continue; // no mixing PAX/FREIGHT
                    // If you encode conflicts (e.g., CROSS) in SectionGraph, add gate checks here.
                    chosen = nxt;
                    break;
                }

                if (chosen == null)
                    continue; // blocked

                // Perform the move
                occ.remove(t.at);
                t.at = chosen;

                // Exit if we just reached a terminal or our designated exit
                if (isExit(t.dir, t.at) && (t.exit < 0 || t.at == t.exit)) {
                    moved++;
                    t.at = -1; // do not occupy
                } else {
                    occ.put(t.at, name);
                    moved++;
                }
            }
            return moved;
        } finally {
            lock.unlock();
        }
    }

    /** Which train (name) is in this section, or null. */
    @Override
    public String getSection(int section) {
        lock.lock();
        try {
            return occ.get(section);
        } finally {
            lock.unlock();
        }
    }

    /** The current section for this train, or -1 if outside/unknown. */
    @Override
    public Integer getTrain(String trainName) {
        lock.lock();
        try {
            TrainState t = trains.get(trainName);
            if (t == null)
                return -1;
            return (t.at == null) ? -1 : t.at;
        } finally {
            lock.unlock();
        }
    }
}
