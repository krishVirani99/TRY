
// InterlockingPublic_Test.java  (place in submission root)
import static org.junit.Assert.*;
import org.junit.Test;

public class InterlockingPublic_Test {
    @Test
    public void add_and_first_move() {
        Interlocking il = new InterlockingImpl();
        assertTrue(il.addTrain("t1", 1, 9));
        Integer moved = il.moveTrains(new String[] { "t1" }); // 1 -> 5
        assertEquals(Integer.valueOf(1), moved);
        assertEquals("t1", il.getSection(5));
        assertEquals(5, il.getTrain("t1"));
    }

    @Test
    public void second_move_stops_at_dest_and_stays() {
        Interlocking il = new InterlockingImpl();
        il.addTrain("t1", 1, 9);
        il.moveTrains(new String[] { "t1" }); // 1 -> 5
        il.moveTrains(new String[] { "t1" }); // 5 -> 9 (should NOT disappear)
        assertEquals("t1", il.getSection(9));
        assertEquals(9, il.getTrain("t1"));
    }

    @Test
    public void concurrent_moves_independent_tracks() {
        Interlocking il = new InterlockingImpl();
        il.addTrain("a", 9, 2); // path 9->5->6->2
        il.addTrain("b", 11, 3);// path 11->7->3
        Integer moved = il.moveTrains(new String[] { "a", "b" });
        assertEquals(Integer.valueOf(2), moved);
        // After one tick: a at 5, b at 7
        assertEquals("a", il.getSection(5));
        assertEquals("b", il.getSection(7));
    }

    @Test
    public void train_at_intermediate_stop_does_not_leave_same_tick() {
        Interlocking il = new InterlockingImpl();
        il.addTrain("x", 9, 2); // x will stage at 6 along the way
        il.moveTrains(new String[] { "x" }); // 9->5
        il.moveTrains(new String[] { "x" }); // 5->6 (stage)
        // same tick should not continue past the stop:
        Integer moved = il.moveTrains(new String[] { "x" }); // blocked at stop this tick
        assertTrue(moved == 0 || moved == null);
        assertEquals(6, il.getTrain("x"));
    }

    @Test
    public void blocked_when_no_next_section_instead_of_removed() {
        Interlocking il = new InterlockingImpl();
        il.addTrain("p", 1, 8);
        // occupy 9 so p cannot go 5->9->8 next tick
        il.addTrain("q", 9, 2);
        il.moveTrains(new String[] { "p", "q" }); // p:1->5, q:9->5 (or stays); at least p reaches 5
        String at5 = il.getSection(5);
        assertNotNull(at5); // someone at 5, system intact
        // most important check: neither train is "out of the system" on a block
        assertTrue(il.getTrain("p") != -1);
        assertTrue(il.getTrain("q") != -1);
    }
}
