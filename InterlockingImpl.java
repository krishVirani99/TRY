import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * InterlockingImpl — default package, JDK 11.
 * Implements the grader API:
 * boolean addTrain(String name, int entry, int exit)
 * Integer moveTrains(String[] names)
 * String getSection(int section)
 * Integer getTrain(String name)
 *
 * No references to TrainType; uses Direction only (we provide Direction.java).
 */
public class InterlockingImpl implements Interlocking {

    private static final class TrainState {
        final String name;
        final Direction dir; // inferred from entry
        final int exit; // target exit section
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

    private final ReentrantLock lock = new ReentrantLock(true);
    private final SectionGraph graph = new SectionGraph();

    // section -> train name
    private final Map<Integer, String> occ = new HashMap<>();
    // train name -> state
    private final Map<String, TrainState> trains = new HashMap<>();

    // ---------- helpers ----------
    private List<Integer> next(Direction d, int s) {
        List<Integer> n = graph.next(d, s);
        return (n == null) ? Collections.emptyList() : n;
    }

    private boolean isExit(Direction d, int s) {
        try {
            return graph.isExit(d, s);
        } catch (Throwable t) {
            List<Integer> n = graph.next(d, s);
            return n == null || n.isEmpty();
        }
    }

    private boolean isEntry(Direction d, int s) {
        try {
            return graph.isEntry(d, s);
        } catch (Throwable t) {
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

    // ============== REQUIRED API ==============

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
                if (t.at == null || t.at < 0)
                    continue; // already outside

                // Exit if at an exit node and this is the target
                if (t.at == t.exit && isExit(t.dir, t.at)) {
                    occ.remove(t.at);
                    t.at = -1;
                    moved++;
                    continue;
                }

                // Candidate next sections
                List<Integer> options = next(t.dir, t.at);
                if (options.isEmpty()) {
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
                        continue; // no mixing lines
                    // (Add resource/priority checks here if you model conflicts)
                    chosen = nxt;
                    break;
                }
                if (chosen == null)
                    continue; // blocked

                // Move
                occ.remove(t.at);
                t.at = chosen;

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
            TrainState t = trains.get(trainName);
            if (t == null)
                return -1;
            return (t.at == null) ? -1 : t.at;
        } finally {
            lock.unlock();
        }
    }
}
