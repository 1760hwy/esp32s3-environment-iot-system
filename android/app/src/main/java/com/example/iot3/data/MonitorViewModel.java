package com.example.iot3.data;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.iot3.network.ReadingSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Owns frontend state and one cloud request at a time, independently of Activity recreation. */
public final class MonitorViewModel extends ViewModel {
    // Placeholder values are never rendered until a real sample has arrived.
    private final MutableLiveData<EnvironmentData> state = new MutableLiveData<>(
            new EnvironmentData(new EnvironmentData.Sample(1, 0, 0, 0), Collections.emptyList(),
                    new EnvironmentData.Device("环境监测站", (com.example.iot3.BuildConfig.NLE_DEVICE_ID.isEmpty() ? "未配置设备" : com.example.iot3.BuildConfig.NLE_DEVICE_ID) + " · ESP32-S3", false)));
    private final MutableLiveData<String> connection = new MutableLiveData<>("等待同步云端数据");
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Runnable poll = this::refreshFromCloud;
    private ReadingSource source;
    private boolean active;
    private boolean cleared;
    private boolean loading;
    private boolean hasReading;
    private String badge = "待同步";
    public boolean autoRefresh = true;
    public int refreshSeconds = 5;
    public int selectedPage = 0;
    public int selectedMetric = 0;
    public int rangeHours = 24;
    public int themeMode = -1;

    public void setSource(ReadingSource source) {
        if (this.source == null) {
            this.source = source;
        }
    }

    public LiveData<EnvironmentData> observe() {
        return state;
    }

    public LiveData<String> observeConnection() {
        return connection;
    }

    public String connectionStatus() {
        return connection.getValue();
    }

    public String connectionBadge() {
        return badge;
    }

    private void setConnection(String message, String badge) {
        this.badge = badge;
        connection.setValue(message);
    }

    public boolean hasReading() {
        return hasReading;
    }

    public boolean isLoading() {
        return loading;
    }

    public EnvironmentData current() {
        return state.getValue();
    }

    public void setActive(boolean active) {
        this.active = active;
        handler.removeCallbacks(poll);
        if (active && autoRefresh) {
            refreshFromCloud();
        }
    }

    public void refreshSchedule() {
        handler.removeCallbacks(poll);
        if (active && autoRefresh && !loading) {
            refreshFromCloud();
        } else if (!autoRefresh && !loading) {
            setConnection(hasReading ? "自动同步已暂停 · 保留上次数据" : "自动同步已暂停", "已暂停");
        }
    }

    public void refreshFromCloud() {
        if (source == null || loading || cleared) {
            return;
        }
        handler.removeCallbacks(poll);
        loading = true;
        setConnection("正在读取云端数据…", "同步中");
        worker.execute(() -> {
            EnvironmentData.Sample sample = null;
            boolean success = false;
            try {
                sample = source.read();
                success = true;
            } catch (Exception ignored) {
                // Never expose credentials, tokens or server response bodies to UI/logcat.
            }
            EnvironmentData.Sample result = sample;
            boolean completed = success;
            handler.post(() -> {
                if (cleared) {
                    return;
                }
                loading = false;
                if (completed) {
                    if (hasReading && result.timestamp < current().current.timestamp) {
                        setConnection("云端采样时间较早 · 保留上次数据", "采样较早");
                    } else {
                        updateReading(result);
                        setConnection(autoRefresh ? "云端数据已同步" : "同步完成 · 自动同步已暂停",
                                autoRefresh ? "云端已同步" : "已暂停");
                    }
                } else {
                    setConnection(hasReading ? "同步失败 · 保留上次数据" : "同步失败 · 请检查网络或云端配置", "同步失败");
                }
                if (active && autoRefresh) {
                    handler.postDelayed(poll, refreshSeconds * 1000L);
                }
            });
        });
    }

    public void updateSnapshot(EnvironmentData snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("Snapshot is required");
        }
        hasReading = true;
        state.setValue(snapshot);
    }

    public void updateReading(EnvironmentData.Sample sample) {
        EnvironmentData old = current();
        if (hasReading && sample.timestamp < old.current.timestamp) {
            return;
        }
        List<EnvironmentData.Sample> history = new ArrayList<>(old.history);
        history.removeIf(item -> item.timestamp == sample.timestamp
                || item.timestamp < sample.timestamp - 7 * 24 * 3600000L);
        history.add(sample);
        while (history.size() > 2000) {
            history.remove(0);
        }
        updateSnapshot(new EnvironmentData(sample, history, old.device));
    }

    public void updateHistory(List<EnvironmentData.Sample> history) {
        EnvironmentData old = current();
        state.setValue(new EnvironmentData(old.current, history, old.device));
    }

    public void updateDevice(EnvironmentData.Device device) {
        EnvironmentData old = current();
        state.setValue(new EnvironmentData(old.current, old.history, device));
    }

    @Override
    protected void onCleared() {
        cleared = true;
        active = false;
        handler.removeCallbacksAndMessages(null);
        worker.shutdownNow();
    }
}
