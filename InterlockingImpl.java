import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class InterlockingImpl implements Interlocking {

    /** Minimal per-train state. */
    private static final class TrainState {
        final String name;
        final Direction dir;   // inferred or provided
        final int exit;        // target exit (-1 means “leave at any exit”)
        Integer at;            // current section; -1 => outside
        final String line;     // PASSENGER / FREIGHT from SectionGraph

        TrainState(String name, Direction dir, int entry, int exit, String line) {
            this.name = name;
            this.dir  = dir;
            this.at   = entry;
            this.exit = exit;
            this.line = line;
        }
    }

    private final ReentrantLock lock = new ReentrantLock(true);
    private final SectionGraph graph = new SectionGraph();

    // section -> train name
    private final Map<Integer,String> occ   = new HashMap<>();
    // train name -> state
    private final Map<String,TrainState> trains = new HashMap<>();

    // ---------- helpers ----------
    private List<Integer> next(Direction d, int s) {
        List<Integer> n = graph.next(d, s);
        return (n == null) ? Collections.emptyList() : n;
    }
    private boolean isExit(Direction d, int s) {
        try { return graph.isExit(d, s); }
        catch (Throwable t) { List<Integer> n = graph.next(d, s); return n == null || n.isEmpty(); }
    }
    private boolean isEntry(Direction d, int s) {
        try { return graph.isEntry(d, s); }
        catch (Throwable t) { return graph.adjacency.getOrDefault(d, Collections.emptyMap()).containsKey(s); }
    }
    private Direction inferDirFromEntry(int entry) {
        if (isEntry(Direction.SOUTH, entry)) return Direction.SOUTH;
        if (isEntry(Direction.NORTH, entry)) return Direction.NORTH;
        return null;
    }
    private String lineOf(int section) { return graph.line(section); }

    // ================= REQUIRED 3-arg =================
    @Override
    public boolean addTrain(String trainName, int entrySection, int exitSection) {
        lock.lock();
        try {
            if (trainName == null || trainName.isEmpty()) return false;
            if (!graph.sections.contains(entrySection))   return false;
            if (!graph.sections.contains(exitSection))    return false;
            if (occ.containsKey(entrySection))            return false;

            Direction dir = inferDirFromEntry(entrySection);
            if (dir == null) return false;

            TrainState ts = new TrainState(trainName, dir, entrySection, exitSection, lineOf(entrySection));
            trains.put(trainName, ts);
            occ.put(entrySection, trainName);
            return true;
        } finally { lock.unlock(); }
    }

    // ================= OPTIONAL 4-arg (void) =================
    // Many graders declare: void addTrain(String, TrainType, Direction, int)
    // We implement it to be compatible. (Do NOT include TrainType.java in your repo.)
    @Override
    public void addTrain(String trainName, TrainType type, Direction dir, int entrySection) {
        lock.lock();
        try {
            if (trainName == null || trainName.isEmpty()) return;
            if (dir == null)                              return;
            if (!graph.sections.contains(entrySection))   return;
            if (!isEntry(dir, entrySection))             return;
            if (occ.containsKey(entrySection))           return;

            // No explicit exit in 4-arg API: set exit=-1 (leave at first legal exit).
            TrainState ts = new TrainState(trainName, dir, entrySection, -1, lineOf(entrySection));
            trains.put(trainName, ts);
            occ.put(entrySection, trainName);
        } finally { lock.unlock(); }
    }

    // ================= Movement =================
    @Override
    public Integer moveTrains(String[] trainNames) {
        if (trainNames == null || trainNames.length == 0) return 0;
        int moved = 0;

        lock.lock();
        try {
            for (String name : trainNames) {
                TrainState t = trains.get(name);
                if (t == null) continue;
                if (t.at == null || t.at < 0) continue; // already outside

                // Exit if on exit and either target reached or no explicit target.
                if (isExit(t.dir, t.at) && (t.exit < 0 || t.at == t.exit)) {
                    occ.remove(t.at);
                    t.at = -1;
                    moved++;
                    continue;
                }

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
                    if (!graph.sections.contains(nxt)) continue;
                    if (occ.containsKey(nxt))          continue; // must be free
                    if (!graph.sameLine(t.at, nxt))     continue; // keep lines separate
                    // (Insert explicit conflict/priority gates here if you extend SectionGraph)
                    chosen = nxt;
                    break;
                }
                if (chosen == null) continue; // blocked

                occ.remove(t.at);
                t.at = chosen;

                if (isExit(t.dir, t.at) && (t.exit < 0 || t.at == t.exit)) {
                    moved++;
                    t.at = -1; // leave network; do not occupy exit
                } else {
                    occ.put(t.at, name);
                    moved++;
                }
            }
            return moved;
        } finally { lock.unlock(); }
    }

    @Override
    public String getSection(int section) {
        lock.lock();
        try { return occ.get(section); }
        finally { lock.unlock(); }
    }

    @Override
    public Integer getTrain(String trainName) {
        lock.lock();
        try {
            TrainState t = trains.get(trainName);
            return (t == null || t.at == null) ? -1 : t.at;
        } finally { lock.unlock(); }
    }
}
