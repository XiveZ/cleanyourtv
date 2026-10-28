package local.tvhome.wallfetch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScheduleTest {
    private static final long DAY = 24L * 60 * 60 * 1000;

    @Test
    public void aPeriodInDaysBecomesMilliseconds() {
        assertEquals(7 * DAY, Schedule.periodMs(7));
        assertEquals(365 * DAY, Schedule.periodMs(365));
    }

    @Test
    public void anUpdateIsDueOnceThePeriodHasPassedOrThereWasNone() {
        assertTrue(Schedule.due(0, 100 * DAY, 7));
        assertFalse(Schedule.due(10 * DAY, 16 * DAY, 7));
        assertTrue(Schedule.due(10 * DAY, 17 * DAY, 7));
        assertFalse(Schedule.due(10 * DAY, 20 * DAY, 0));   // off
    }
}
