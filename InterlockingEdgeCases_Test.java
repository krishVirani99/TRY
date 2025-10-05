import static org.junit.Assert.*;
import org.junit.Test;

public class InterlockingEdgeCases_Test {

    @Test
    public void duplicateIdsRejected() {
        Interlocking il = new InterlockingImpl();
        assertTrue(il.addTrain("T", 1, 9));
        assertFalse(il.addTrain("T", 3, 11)); // same id not allowed
    }

    @Test
    public void occupancyReportedCorrectly() {
        Interlocking il = new InterlockingImpl();
        assertTrue(il.addTrain("P1", 1, 9));
        assertEquals("P1", il.getSection(1));
        assertEquals(1, il.moveTrains(new String[]{"P1"}).intValue()); // 1->5
        assertNull(il.getSection(1));
        assertEquals("P1", il.getSection(5));
    }
}
