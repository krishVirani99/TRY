import static org.junit.Assert.*;
import org.junit.Test;

public class InterlockingPublic_Test {
    @Test
    public void passengerAddMoveExit() {
        Interlocking il = new InterlockingImpl();
        assertTrue(il.addTrain("P", 1, 9));
        assertEquals(Integer.valueOf(1), il.moveTrains(new String[] { "P" })); // 1->5
        assertEquals(Integer.valueOf(1), il.moveTrains(new String[] { "P" })); // 5->9 then exit
        assertEquals(-1, il.getTrain("P"));
    }

    @Test
    public void followTheLeaderSameTick() {
        Interlocking il = new InterlockingImpl();
        assertTrue(il.addTrain("A", 9, 2)); // 9->5->6->2
        assertTrue(il.addTrain("B", 5, 9)); // 5->9
        il.moveTrains(new String[] { "A", "B" }); // concurrent: A 9->5 while B 5->9
        assertEquals("A", il.getSection(5));
        assertEquals("B", il.getSection(9));
    }

    @Test
    public void swapAdjacentIsAllowed() {
        Interlocking il = new InterlockingImpl();
        assertTrue(il.addTrain("X", 5, 6));
        assertTrue(il.addTrain("Y", 6, 5));
        assertEquals(Integer.valueOf(2), il.moveTrains(new String[] { "X", "Y" }));
        assertEquals("X", il.getSection(6));
        assertEquals("Y", il.getSection(5));
    }

    @Test
    public void freightYieldsAtDiamondWhenPassengerStaged() {
        Interlocking il = new InterlockingImpl();
        assertTrue(il.addTrain("P", 1, 9));
        il.moveTrains(new String[] { "P" }); // P to 5 (staged)
        assertTrue(il.addTrain("F", 11, 3)); // freight wants 11->7
        int before = il.getTrain("F");
        il.moveTrains(new String[] { "F" }); // blocked this tick
        assertEquals(before, il.getTrain("F"));
    }
}
