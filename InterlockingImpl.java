import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gradescope-ready Interlocking implementation.
 * Implements the grader API:
 * - boolean addTrain(String trainName, int entrySection, int exitSection)
 * - Integer moveTrains(String[] trainNames)
 * - String getSection(int section)
 * - Integer getTrain(String trainName)
 *
 * Default package (no package line).
 * Relies on the grader-provided Interlocking/Direction/TrainType classes.
 * Uses SectionGraph for adjacency, entries, exits, line separation.
 */
public class InterlockingImpl implements Interlocking {

    /** Minimal per-train state. */
    static final class TrainInfo {
        final String name;
        final Direction dir; // inferred from entry
        final int exit; // target exit section
        Integer at; // current section, -1 when outside
        final String line; // "PASSENGER" or "FREIGHT" from SectionGraph

        TrainInfo(String name, Direction dir, int entry, int exit, String line) {
            this.name = name;
            this.dir = dir;
            this.at = entry;
            this.exit = exit;
            this.line = line;
        }
    }

    private final ReentrantLock lock = new ReentrantLock(true);
    private final SectionGraph graph = new SectionGraph();

    // section -> train name
    private final Map<Integer, String> occ = new HashMap<>();
    // train name -> state
    private final Map<String, TrainInfo> trains = new HashMap<>();

    // ---------- helpers ----------
    private boolean isExit(Direction d, int s) {
        try {
            return graph.isExit(d, s);
        } catch (Throwable t) {
            List<Integer> nxt = graph.next(d, s);
            return nxt == null || nxt.isEmpty();
        }
    }

    private boolean isEntry(Direction d, int s) {
        try {
            return graph.isEntry(d, s);
        } catch (Throwable t) {
            return true;
        }
    }

    private List<Integer> next(Direction d, int s) {
        List<Integer> n = graph.next(d, s);
        return (n == null) ? Collections.emptyList() : n;
    }

    private Direction inferDirFromEntry(int entry) {
        if (isEntry(Direction.SOUTH, entry))
            return Direction.SOUTH;
        if (isEntry(Direction.NORTH, entry))
            return Direction.NORTH;
        return null;
    }

    // ========== REQUIRED API ==========

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

            TrainInfo t = new TrainInfo(trainName, dir, entrySection, exitSection, line);
            trains.put(trainName, t);
            occ.put(entrySection, trainName);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Integer moveTrains(String[] trainNames) {
        if (trainNames == null || trainNames.length == 0)
            return 0;
        int moved = 0;

        lock.lock();
        try {
            for (String name : trainNames) {
                TrainInfo t = trains.get(name);
                if (t == null)
                    continue;
                if (t.at == null || t.at < 0)
                    continue; // already outside

                // Exit immediately if sitting on an exit node that matches target
                if (t.at == t.exit && isExit(t.dir, t.at)) {
                    occ.remove(t.at);
                    t.at = -1;
                    moved++;
                    continue;
                }

                // legal next sections
                List<Integer> options = next(t.dir, t.at);
                if (options.isEmpty()) {
                    // dead-end: allow exit only if graph marks it as exit
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
                        continue; // keep lines separate
                    // (Add explicit conflict/priority checks here if you model CROSS as a resource)
                    chosen = nxt;
                    break;
                }

                if (chosen == null)
                    continue; // blocked

                // move
                occ.remove(t.at);
                t.at = chosen;

                // if moved into target exit and it's an exit, leave
                if (t.at == t.exit && isExit(t.dir, t.at)) {
                    moved++;
                    t.at = -1;
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

    @Override
    public String getSection(int section) {
        lock.lock();
        try {
            return occ.get(section);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Integer getTrain(String trainName) {
        lock.lock();
        try {
            TrainInfo t = trains.get(trainName);
            if (t == null)
                return -1;
            return (t.at == null) ? -1 : t.at;
        } finally {
            lock.unlock();
        }
    }
}
