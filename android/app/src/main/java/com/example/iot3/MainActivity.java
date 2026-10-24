package com.example.iot3;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.iot3.data.EnvironmentData;
import com.example.iot3.data.MonitorViewModel;
import com.example.iot3.network.NleCloudSource;
import com.example.iot3.ui.DeviceSettings;
import com.example.iot3.ui.HistoryPanel;
import com.example.iot3.ui.TrendChartView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private MonitorViewModel model;
    private View dashboard;
    private View history;
    private View devices;
    private HistoryPanel historyPanel;
    private DeviceSettings deviceSettings;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = new ViewModelProvider(this).get(MonitorViewModel.class);
        configureCloud();
        if (savedInstanceState != null) {
            model.selectedPage = savedInstanceState.getInt("page", 0);
            model.selectedMetric = savedInstanceState.getInt("metric", 0);
            model.rangeHours = savedInstanceState.getInt("range", 24);
            model.autoRefresh = savedInstanceState.getBoolean("autoRefresh", true);
            model.refreshSeconds = savedInstanceState.getInt("interval", 5);
            model.themeMode = savedInstanceState.getInt("theme", -1);
            EnvironmentData.Device current = model.current().device;
            model.updateDevice(new EnvironmentData.Device(
                    savedInstanceState.getString("name", current.name), current.id, current.online));
        }
        DeviceSettings.applyTheme(this, model.themeMode);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        // Pages live in XML so Android Studio can preview the actual screen hierarchy.
        dashboard = findViewById(R.id.dashboard_page);
        history = findViewById(R.id.history_page);
        devices = findViewById(R.id.devices_page);
        historyPanel = new HistoryPanel(history, model);
        deviceSettings = new DeviceSettings(this, devices, model, model::refreshSchedule);
        BottomNavigationView navigation = findViewById(R.id.bottom_navigation);
        navigation.setOnItemSelectedListener(item -> {
            model.selectedPage = item.getItemId() == R.id.nav_home ? 0
                    : item.getItemId() == R.id.nav_history ? 1 : 2;
            showPage();
            return true;
        });
        navigation.setSelectedItemId(model.selectedPage == 0 ? R.id.nav_home
                : model.selectedPage == 1 ? R.id.nav_history : R.id.nav_devices);
        dashboard.findViewById(R.id.open_history).setOnClickListener(view ->
                navigation.setSelectedItemId(R.id.nav_history));
        dashboard.findViewById(R.id.refresh).setOnClickListener(view -> model.refreshFromCloud());
        model.observe().observe(this, this::render);
        model.observeConnection().observe(this, ignored -> render(model.current()));
        showPage();
    }

    private void showPage() {
        dashboard.setVisibility(model.selectedPage == 0 ? View.VISIBLE : View.GONE);
        history.setVisibility(model.selectedPage == 1 ? View.VISIBLE : View.GONE);
        devices.setVisibility(model.selectedPage == 2 ? View.VISIBLE : View.GONE);
    }

    private void render(EnvironmentData data) {
        setText(dashboard, R.id.station_name, data.device.name);
        setText(dashboard, R.id.online_status, model.connectionBadge());
        setText(dashboard, R.id.temperature_value, model.hasReading() ? String.format(Locale.CHINA, "%.1f°", data.current.temperature) : "—");
        setText(dashboard, R.id.humidity_value, model.hasReading() ? String.format(Locale.CHINA, "%.1f", data.current.humidity) : "—");
        setText(dashboard, R.id.light_value, model.hasReading() ? String.format(Locale.CHINA, "%.0f", data.current.illuminance) : "—");
        String timestamp = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA)
                .format(new Date(data.current.timestamp));
        setText(dashboard, R.id.updated_at, model.hasReading() ? "采样于 " + timestamp + " · " + model.connectionStatus() : model.connectionStatus());
        setText(dashboard, R.id.dashboard_device, "  " + data.device.id + " · 3 项环境指标");
        dashboard.findViewById(R.id.refresh).setEnabled(!model.isLoading());
        setText(dashboard, R.id.refresh, model.isLoading() ? "同步中" : "刷新");
        TrendChartView mini = dashboard.findViewById(R.id.mini_chart);
        mini.setSeries(HistoryPanel.filter(data, 24), 0, true);
        historyPanel.render(data);
        deviceSettings.render(data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        model.setActive(true);
    }

    @Override
    protected void onPause() {
        model.setActive(false);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("page", model.selectedPage);
        outState.putInt("metric", model.selectedMetric);
        outState.putInt("range", model.rangeHours);
        outState.putBoolean("autoRefresh", model.autoRefresh);
        outState.putInt("interval", model.refreshSeconds);
        outState.putInt("theme", model.themeMode);
        outState.putString("name", model.current().device.name);
        super.onSaveInstanceState(outState);
    }

    public static void setText(View root, int id, String text) {
        ((TextView) root.findViewById(id)).setText(text);
    }

    private void configureCloud() {
        // Local tutorial configuration; never distribute an APK containing real credentials.
        String account = BuildConfig.NLE_ACCOUNT;
        String password = BuildConfig.NLE_PASSWORD;
        model.setSource(new NleCloudSource(account, password));
    }
}
