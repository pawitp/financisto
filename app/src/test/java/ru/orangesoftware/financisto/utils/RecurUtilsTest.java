package ru.orangesoftware.financisto.utils;

import org.junit.Test;
import ru.orangesoftware.financisto.datetime.Period;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.utils.RecurUtils.RecurInterval;

import static org.junit.Assert.*;

public class RecurUtilsTest {

    @Test
    public void should_calculate_yearly_period_correctly() {
        // Test yearly period calculation
        long startDate = DateTime.date(2011, 1, 1).atMidnight().asLong();
        Period yearlyPeriod = RecurInterval.YEARLY.next(startDate);
        
        // Should start at beginning of year
        assertEquals(startDate, yearlyPeriod.start);
        
        // Should end at end of same year (Dec 31, 2011 23:59:59.999)
        long expectedEnd = DateTime.date(2011, 12, 31).at(23, 59, 59, 999).asLong();
        assertEquals(expectedEnd, yearlyPeriod.end);
    }

    @Test
    public void should_calculate_yearly_period_from_mid_year() {
        // Test yearly period starting from mid-year
        long startDate = DateTime.date(2011, 6, 15).atMidnight().asLong();
        Period yearlyPeriod = RecurInterval.YEARLY.next(startDate);
        
        // Should start at the provided date
        assertEquals(startDate, yearlyPeriod.start);
        
        // Should end one year minus one day later (June 14, 2012 23:59:59.999)
        long expectedEnd = DateTime.date(2012, 6, 14).at(23, 59, 59, 999).asLong();
        assertEquals(expectedEnd, yearlyPeriod.end);
    }

    @Test
    public void should_handle_leap_year_correctly() {
        // Test yearly period in leap year (2012)
        long startDate = DateTime.date(2012, 1, 1).atMidnight().asLong();
        Period yearlyPeriod = RecurInterval.YEARLY.next(startDate);
        
        // Should start at beginning of leap year
        assertEquals(startDate, yearlyPeriod.start);
        
        // Should end at end of leap year (Dec 31, 2012 23:59:59.999)
        long expectedEnd = DateTime.date(2012, 12, 31).at(23, 59, 59, 999).asLong();
        assertEquals(expectedEnd, yearlyPeriod.end);
    }
}