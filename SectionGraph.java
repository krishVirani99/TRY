import java.util.*;

/**
 * Section-level graph and constraints loaded from configs/islington.json
 * (serialized in code for simplicity).
 */
class SectionGraph {
    final Set<Integer> sections = new HashSet<>();
    final Map<Direction, Map<Integer, List<Integer>>> adjacency = new EnumMap<>(Direction.class);
    final Map<Direction, Set<Integer>> entries = new EnumMap<>(Direction.class);
    final Map<Direction, Set<Integer>> exits = new EnumMap<>(Direction.class);
    final Map<Integer, String> lineOfSection = new HashMap<>(); // PASSENGER/FREIGHT

    // conflict resource -> (moves by type)
    static class ConflictDef {
        boolean passengerPriority = true;
        List<int[]> passengerMoves = new ArrayList<>();
        List<int[]> freightMoves = new ArrayList<>();
    }

    final Map<String, ConflictDef> conflicts = new HashMap<>();

    SectionGraph() {
        // Embedded config mirrors configs/islington.json (kept in code for simple
        // build)
        for (int i = 1; i <= 11; i++)
            sections.add(i);

        Map<Integer, List<Integer>> south = new HashMap<>();
        south.put(1, Arrays.asList(5, 6));
        south.put(5, Arrays.asList(8));
        south.put(6, Arrays.asList(9));
        south.put(3, Arrays.asList(7));
        south.put(7, Arrays.asList(11));
        south.put(4, Collections.emptyList());
        south.put(8, Collections.emptyList());
        south.put(9, Collections.emptyList());
        south.put(10, Collections.emptyList());
        south.put(11, Collections.emptyList());
        south.put(2, Collections.emptyList());

        Map<Integer, List<Integer>> north = new HashMap<>();
        north.put(9, Arrays.asList(6, 5));
        north.put(10, Arrays.asList(6));
        north.put(4, Arrays.asList(5));
        north.put(11, Arrays.asList(7));
        north.put(7, Arrays.asList(3));
        north.put(5, Arrays.asList(2));
        north.put(6, Arrays.asList(2));
        north.put(2, Collections.emptyList());
        north.put(3, Collections.emptyList());
        north.put(1, Collections.emptyList());

        adjacency.put(Direction.SOUTH, south);
        adjacency.put(Direction.NORTH, north);

        entries.put(Direction.SOUTH, new HashSet<>(Arrays.asList(1, 3)));
        entries.put(Direction.NORTH, new HashSet<>(Arrays.asList(4, 9, 10, 11)));
        exits.put(Direction.SOUTH, new HashSet<>(Arrays.asList(4, 8, 9, 11)));
        exits.put(Direction.NORTH, new HashSet<>(Arrays.asList(2, 3)));

        // line labels
        for (int s : Arrays.asList(1, 2, 4, 5, 6, 8, 9, 10))
            lineOfSection.put(s, "PASSENGER");
        for (int s : Arrays.asList(3, 7, 11))
            lineOfSection.put(s, "FREIGHT");

        // conflicts
        ConflictDef cross = new ConflictDef();
        cross.passengerPriority = true;
        cross.passengerMoves.add(new int[] { 6, 9 });
        cross.passengerMoves.add(new int[] { 5, 8 });
        cross.passengerMoves.add(new int[] { 9, 6 });
        cross.passengerMoves.add(new int[] { 4, 5 });
        cross.freightMoves.add(new int[] { 7, 11 });
        cross.freightMoves.add(new int[] { 11, 7 });
        cross.freightMoves.add(new int[] { 7, 3 });
        conflicts.put("CROSS", cross);
    }

    boolean isEntry(Direction d, int sect) {
        return entries.get(d).contains(sect);
    }

    boolean isExit(Direction d, int sect) {
        return exits.get(d).contains(sect);
    }

    List<Integer> next(Direction d, int sect) {
        return adjacency.getOrDefault(d, Collections.emptyMap())
                .getOrDefault(sect, Collections.emptyList());
    }

    String line(int section) {
        return lineOfSection.get(section);
    }

    boolean sameLine(int a, int b) {
        return Objects.equals(lineOfSection.get(a), lineOfSection.get(b));
    }
}

