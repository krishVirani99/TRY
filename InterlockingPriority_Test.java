import static org.junit.Assert.*;
import org.junit.Test;

public class InterlockingPriority_Test {

    @Test
    public void passengerHasPriorityAtDiamond() {
        Interlocking il = new InterlockingImpl();

        // Freight staged at 7
        assertTrue(il.addTrain("F1", 3, 11));
        assertEquals(1, il.moveTrains(new String[]{"F1"}).intValue()); // 3->7

        // Passenger staged at 6
        assertTrue(il.addTrain("P1", 1, 9));
        assertEquals(1, il.moveTrains(new String[]{"P1"}).intValue()); // 1->5
        assertEquals(1, il.moveTrains(new String[]{"P1"}).intValue()); // 5->6

        // Freight blocked while passenger staged
        assertEquals(0, il.moveTrains(new String[]{"F1"}).intValue()); // 7->11 blocked

        // Passenger exits
        assertEquals(1, il.moveTrains(new String[]{"P1"}).intValue()); // 6->9 exit
        assertEquals(-1, il.getTrain("P1"));

        // Freight follows
        assertEquals(1, il.moveTrains(new String[]{"F1"}).intValue()); // 7->11 exit
        assertEquals(-1, il.getTrain("F1"));
    }
}
