package com.example.iot3.data;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class EnvironmentDataTest {
    @Test
    public void snapshotOwnsItsHistoryAndExposesReadOnlyData() {
        List<EnvironmentData.Sample> source = new ArrayList<>();
        source.add(new EnvironmentData.Sample(1000, 24, 56, 420));
        EnvironmentData data = new EnvironmentData(source.get(0), source,
                new EnvironmentData.Device("环境监测站", "ESP32-S3-01", true));
        source.clear();
        assertEquals(1, data.history.size());
        assertThrows(UnsupportedOperationException.class, () -> data.history.clear());
    }

    @Test
    public void invalidSensorValuesAreRejectedAtTheUpdateBoundary() {
        assertThrows(IllegalArgumentException.class,
                () -> new EnvironmentData.Sample(1000, Double.NaN, 50, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new EnvironmentData.Sample(1000, 24, 101, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new EnvironmentData.Sample(1000, 24, 50, -1));
    }

    @Test
    public void demoHistoryIsOrderedAndEndsAtCurrentSample() {
        EnvironmentData data = EnvironmentData.demo(1800000000000L);
        assertEquals(169, data.history.size());
        assertEquals(data.current, data.history.get(data.history.size() - 1));
        for (int i = 1; i < data.history.size(); i++) {
            assertTrue(data.history.get(i).timestamp > data.history.get(i - 1).timestamp);
        }
        assertEquals(7, (data.current.timestamp - data.history.get(0).timestamp)
                / (24 * 60 * 60 * 1000L));
    }
}
