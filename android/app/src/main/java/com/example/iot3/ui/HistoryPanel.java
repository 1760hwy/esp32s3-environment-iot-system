package com.example.iot3.ui;

import android.view.View;
import com.example.iot3.MainActivity;
import com.example.iot3.R;
import com.example.iot3.data.EnvironmentData;
import com.example.iot3.data.MonitorViewModel;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HistoryPanel {
    private final View root;
    private final MonitorViewModel model;

    public HistoryPanel(View root, MonitorViewModel model) {
        this.root = root;
        this.model = model;
        ChipGroup metrics = root.findViewById(R.id.metric_group);
        metrics.check(model.selectedMetric == 0 ? R.id.metric_temp
                : model.selectedMetric == 1 ? R.id.metric_humidity : R.id.metric_light);
        ChipGroup ranges = root.findViewById(R.id.range_group);
        ranges.check(model.rangeHours == 6 ? R.id.range_6
                : model.rangeHours == 24 ? R.id.range_24 : R.id.range_168);
        metrics.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                model.selectedMetric = id == R.id.metric_temp ? 0 : id == R.id.metric_humidity ? 1 : 2;
                render(model.current());
            }
        });
        ranges.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                model.rangeHours = id == R.id.range_6 ? 6 : id == R.id.range_24 ? 24 : 168;
                render(model.current());
            }
        });
    }

    public static List<EnvironmentData.Sample> filter(EnvironmentData data, int hours) {
        long start = data.current.timestamp - hours * 3600000L;
        List<EnvironmentData.Sample> result = new ArrayList<>();
        for (EnvironmentData.Sample sample : data.history) {
            if (sample.timestamp >= start && sample.timestamp <= data.current.timestamp) {
                result.add(sample);
            }
        }
        return result;
    }

    public void render(EnvironmentData data) {
        int metric = model.selectedMetric;
        String name = metric == 0 ? "温度" : metric == 1 ? "湿度" : "光照";
        String unit = metric == 0 ? "°C" : metric == 1 ? "%" : "lx";
        MainActivity.setText(root, R.id.chart_title, name + "趋势");
        MainActivity.setText(root, R.id.chart_unit, unit);
        List<EnvironmentData.Sample> series = filter(data, model.rangeHours);
        MainActivity.setText(root, R.id.chart_period,
                (model.rangeHours == 168 ? "近 7 天" : "近 " + model.rangeHours + " 小时")
                        + " · " + series.size() + " 个真实采样点");
        ((TrendChartView) root.findViewById(R.id.history_chart)).setSeries(series, metric, false);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double sum = 0;
        for (EnvironmentData.Sample sample : series) {
            double value = sample.value(metric);
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
        }
        int[] ids = {R.id.stat_min, R.id.stat_avg, R.id.stat_max};
        double[] values = {min, series.isEmpty() ? 0 : sum / series.size(), max};
        for (int i = 0; i < ids.length; i++) {
            MainActivity.setText(root, ids[i], series.isEmpty() ? "--"
                    : String.format(Locale.CHINA, metric == 2 ? "%.0f" : "%.1f", values[i]));
        }
    }
}
