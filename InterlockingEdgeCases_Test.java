import static org.junit.Assert.*;
import org.junit.Test;

public class InterlockingEdgeCases_Test {

    @Test
    public void noMixingOfLines() {
        Interlocking il = new InterlockingImpl();
        // freight entries are 3/11; passenger entries include 1/4/9/10
        assertFalse(il.addTrain("PX", 3, 11)); // passenger at freight entry -> false (inferred freight)
        assertFalse(il.addTrain("FX", 1, 9));  // freight at passenger entry -> false (inferred passenger)
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
