package com.example.iot3;

import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.iot3.data.EnvironmentData;
import com.example.iot3.data.MonitorViewModel;
import com.example.iot3.ui.DeviceSettings;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.materialswitch.MaterialSwitch;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Collections;
import static org.junit.Assert.*;

/** Native view tests avoid the template Espresso 3.5.1 input-injection API on API 37. */
@RunWith(AndroidJUnit4.class)
public class FrontendFlowTest {
    private String text(MainActivity activity, int id) {
        return ((TextView) activity.findViewById(id)).getText().toString();
    }

    @Test
    public void navigationFiltersAndRecreationKeepSelection() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(activity.findViewById(R.id.temperature_value).isShown());
                ((BottomNavigationView) activity.findViewById(R.id.bottom_navigation))
                        .setSelectedItemId(R.id.nav_history);
                activity.findViewById(R.id.metric_light).performClick();
                activity.findViewById(R.id.range_168).performClick();
                assertEquals("光照趋势", text(activity, R.id.chart_title));
                assertEquals("lx", text(activity, R.id.chart_unit));
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertTrue(((Chip) activity.findViewById(R.id.metric_light)).isChecked());
                assertTrue(((Chip) activity.findViewById(R.id.range_168)).isChecked());
                assertTrue(activity.findViewById(R.id.chart_title).isShown());
                ((BottomNavigationView) activity.findViewById(R.id.bottom_navigation))
                        .setSelectedItemId(R.id.nav_devices);
                assertEquals("环境监测站", text(activity, R.id.device_name));
            });
        }
    }

    @Test
    public void externalUpdatesAndEmptyHistoryReachTheViews() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                MonitorViewModel model = new ViewModelProvider(activity).get(MonitorViewModel.class);
                model.autoRefresh = false;
                model.setActive(false);
                model.updateReading(new EnvironmentData.Sample(Math.max(System.currentTimeMillis(), model.current().current.timestamp) + 1000,
                        31.2, 63.4, 999));
                model.updateHistory(Collections.emptyList());
                model.updateDevice(new EnvironmentData.Device("测试监测站", "TEST-01", false));
                assertEquals("31.2°", text(activity, R.id.temperature_value));
                assertFalse(text(activity, R.id.online_status).contains("模拟"));
                assertEquals("--", text(activity, R.id.stat_avg));
                assertEquals("测试监测站", text(activity, R.id.device_name));
            });
        }
    }

    @Test
    public void darkThemeAndPausedCloudSyncSurviveRecreation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                MonitorViewModel model = new ViewModelProvider(activity).get(MonitorViewModel.class);
                ((BottomNavigationView) activity.findViewById(R.id.bottom_navigation))
                        .setSelectedItemId(R.id.nav_devices);
                ((MaterialSwitch) activity.findViewById(R.id.demo_switch)).performClick();
                model.themeMode = AppCompatDelegate.MODE_NIGHT_YES;
                DeviceSettings.applyTheme(activity, model.themeMode);
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertEquals("显示主题 · 深色", text(activity, R.id.theme));
                assertFalse(((MaterialSwitch) activity.findViewById(R.id.demo_switch)).isChecked());
                assertTrue(activity.findViewById(R.id.device_title).isShown());
            });
        }
    }
}
