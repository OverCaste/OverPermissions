package com.overmc.overpermissions.misctests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.overmc.overpermissions.exceptions.TimeFormatException;
import com.overmc.overpermissions.internal.TimeUtils;

public class TimeUtilTests {
    @Test
    public void testTimeIntegers( ) {
        assertEquals("5m parsed should be 5*60*1000 = 300000ms", TimeUtils.parseMilliseconds("5m"), 5L*60L*1000L);
        assertEquals("5m12s parsed should be 5*60*1000+12*1000 = 312000ms", TimeUtils.parseMilliseconds("5m12s"), 5L*60L*1000L+12L*1000L);
        assertEquals("4d parsed should be 4*24*60*60*1000=345600000ms", TimeUtils.parseMilliseconds("4d"), 4L*24L*60L*60L*1000L);
    }
    
    @Test
    public void testTimeDecimals( ) {
        assertEquals(".5yr parsed should be (365/2)*24*60*60*1000=15768000000ms", TimeUtils.parseMilliseconds(".5yr"), 365L*24L*60L*60L*1000L/2L);
        assertEquals("100yrs parsed should be 100*365*24*60*60*1000=3153600000000ms", TimeUtils.parseMilliseconds("100yrs"), 100L*365L*24L*60L*60L*1000L);
    }
    
    @Test(expected = TimeFormatException.class)
    public void testInvalidTimeUnit( ) {
        TimeUtils.parseMilliseconds("5mw");
    }
    
    @Test(expected = TimeFormatException.class)
    public void testInvalidTimeValue( ) {
        TimeUtils.parseMilliseconds("1.0.0ms");
    }
}
