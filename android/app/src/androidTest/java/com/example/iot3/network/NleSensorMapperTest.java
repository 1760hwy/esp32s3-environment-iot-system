package com.example.iot3.network;

import com.example.iot3.data.EnvironmentData;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class NleSensorMapperTest {
    private String sensor(String tag, String value, String time) {
        return "{\"ApiTag\":\"" + tag + "\",\"Value\":" + value
                + ",\"RecordTime\":\"" + time + "\"}";
    }

    private JSONObject response(String temperature, String humidity, String light) throws Exception {
        return new JSONObject("{\"Status\":0,\"ResultObj\":["
                + sensor("Light", light, "2026-10-23 11:34:02") + ","
                + sensor("Temperature", temperature, "2026-10-23 11:34:00") + ","
                + sensor("Humidity", humidity, "2026-10-23 11:34:01") + "]}");
    }

    @Test
    public void mapsTagsRegardlessOfOrderAndUsesOldestMeasurementTime() throws Exception {
        EnvironmentData.Sample reading = NleSensorMapper.parse(response("22.97", "\"62.58\"", "472.5"));
        assertEquals(22.97, reading.temperature, 0.001);
        assertEquals(62.58, reading.humidity, 0.001);
        assertEquals(472.5, reading.illuminance, 0.001);
        assertEquals(1792726440000L, reading.timestamp); // Asia/Shanghai, not phone timezone.
    }

    @Test
    public void rejectsMissingNullInvalidAndUnsuccessfulResponses() throws Exception {
        assertThrows(Exception.class, () -> NleSensorMapper.parse(response("null", "60", "400")));
        assertThrows(Exception.class, () -> NleSensorMapper.parse(response("22", "101", "400")));
        assertThrows(Exception.class, () -> NleSensorMapper.parse(response("22", "60", "-1")));
        assertThrows(Exception.class, () -> NleSensorMapper.parse(new JSONObject("{\"Status\":0,\"ResultObj\":[]}")));
        assertThrows(Exception.class, () -> NleSensorMapper.parse(new JSONObject("{\"Status\":1}")));
        JSONObject invalidTime = response("22", "60", "400");
        invalidTime.getJSONArray("ResultObj").getJSONObject(0).put("RecordTime", "invalid");
        assertThrows(Exception.class, () -> NleSensorMapper.parse(invalidTime));
    }
}
