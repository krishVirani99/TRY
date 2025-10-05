import static org.junit.Assert.*;
import org.junit.Test;

public class InterlockingBasic_Test {

    @Test
    public void passengerAddMoveExit() {
        Interlocking il = new InterlockingImpl();
        assertTrue(il.addTrain("P1", 1, 9));          // 1->9
        assertEquals(1, il.getTrain("P1"));
        int m1 = il.moveTrains(new String[]{"P1"}); // 1->5
        assertEquals(1, m1);
        assertEquals("P1", il.getSection(5));
        int m2 = il.moveTrains(new String[]{"P1"}); // 5->6
        assertEquals(1, m2);
        int m3 = il.moveTrains(new String[]{"P1"}); // 6->9 (exit)
        assertEquals(1, m3);
        assertEquals(-1, il.getTrain("P1"));
    }

    @Test
    public void capacityAndSecondTrain() {
        Interlocking il = new InterlockingImpl();
        assertTrue(il.addTrain("P1", 1, 9));
        assertFalse(il.addTrain("P2", 1, 9)); // occupied
        int m1 = il.moveTrains(new String[]{"P1"}); // to 5
        assertEquals(1, m1);
        assertTrue(il.addTrain("P2", 1, 9)); // now free
    }

    @Test
    public void freightLineThrough() {
        Interlocking il = new InterlockingImpl();
        assertTrue(il.addTrain("F1", 3, 11));
        assertEquals(1, il.moveTrains(new String[]{"F1"}).intValue()); // 3->7
        assertEquals(1, il.moveTrains(new String[]{"F1"}).intValue()); // 7->11 (exit)
        assertEquals(-1, il.getTrain("F1"));
    }
}
