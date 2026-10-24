package com.example.iot3.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable frontend snapshot. Contains no transport, persistence or platform credentials. */
public final class EnvironmentData {
    public final Sample current;
    public final List<Sample> history;
    public final Device device;

    public EnvironmentData(Sample current, List<Sample> history, Device device) {
        if (current == null || history == null || device == null) {
            throw new IllegalArgumentException("Snapshot fields must not be null");
        }
        this.current = current;
        List<Sample> ordered = new ArrayList<>(history);
        ordered.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
        this.history = Collections.unmodifiableList(ordered);
        this.device = device;
    }

    public static final class Sample {
        public final long timestamp;
        public final double temperature;
        public final double humidity;
        public final double illuminance;

        public Sample(long timestamp, double temperature, double humidity, double illuminance) {
            if (timestamp <= 0 || !Double.isFinite(temperature) || !Double.isFinite(humidity)
                    || !Double.isFinite(illuminance) || humidity < 0 || humidity > 100
                    || illuminance < 0) {
                throw new IllegalArgumentException("Invalid environmental sample");
            }
            this.timestamp = timestamp;
            this.temperature = temperature;
            this.humidity = humidity;
            this.illuminance = illuminance;
        }

        public double value(int metric) {
            return metric == 1 ? humidity : metric == 2 ? illuminance : temperature;
        }
    }

    public static final class Device {
        public final String name;
        public final String id;
        public final boolean online;

        public Device(String name, String id, boolean online) {
            if (name == null || name.trim().isEmpty() || id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Device name and id are required");
            }
            this.name = name.trim();
            this.id = id.trim();
            this.online = online;
        }
    }

    public static EnvironmentData demo(long now) {
        List<Sample> samples = new ArrayList<>();
        for (int i = 0; i <= 168; i++) {
            double phase = i * Math.PI / 12;
            samples.add(new Sample(now - (168 - i) * 3600000L,
                    24.6 + 1.8 * Math.sin(phase) + 0.4 * Math.sin(i * 1.7),
                    58 + 6 * Math.sin(phase + 1) + 1.2 * Math.cos(i),
                    426 + 160 * Math.sin(phase - 0.6) + 32 * Math.cos(i * 0.8)));
        }
        Sample current = new Sample(now, 24.6, 58.2, 426);
        samples.set(samples.size() - 1, current);
        return new EnvironmentData(current, samples,
                new Device("环境监测站", "ESP32-S3-01", true));
    }
}
