package com.example.iot3.network;

import androidx.test.platform.app.InstrumentationRegistry;
import com.example.iot3.data.EnvironmentData;
import com.example.iot3.data.MonitorViewModel;
import org.junit.Test;
import static org.junit.Assert.*;

public class CloudModelTest {
    @Test
    public void startsWithoutDemoAndAcceptsCloudTimeBehindPhoneClock() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            MonitorViewModel model = new MonitorViewModel();
            assertFalse(model.hasReading());
            assertTrue(model.current().history.isEmpty());
            model.updateDevice(new EnvironmentData.Device("监测站", "YOUR_DEVICE_ID", false));
            assertFalse(model.hasReading());
            model.updateReading(new EnvironmentData.Sample(1000, 22.9, 62.5, 472.5));
            assertTrue(model.hasReading());
            assertEquals(22.9, model.current().current.temperature, 0.001);
            assertEquals(1, model.current().history.size());
            model.updateReading(new EnvironmentData.Sample(1000, 23.0, 62.5, 472.5));
            assertEquals(1, model.current().history.size());
            assertEquals(23.0, model.current().current.temperature, 0.001);
            model.updateReading(new EnvironmentData.Sample(900, 10, 30, 100));
            assertEquals(1000, model.current().current.timestamp);
            model.updateReading(new EnvironmentData.Sample(2000, 24, 61, 470));
            assertEquals(2, model.current().history.size());
        });
    }
}
