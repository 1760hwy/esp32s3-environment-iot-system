package com.example.iot3.network;

import com.example.iot3.data.EnvironmentData;

/** A synchronous read boundary; MonitorViewModel always calls it on its worker thread. */
public interface ReadingSource {
    EnvironmentData.Sample read() throws Exception;
}
