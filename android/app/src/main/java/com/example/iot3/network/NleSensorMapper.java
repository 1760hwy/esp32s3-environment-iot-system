package com.example.iot3.network;

import com.example.iot3.data.EnvironmentData;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Maps the existing NLECloud sensor response, never substitutes mock or zero readings. */
public final class NleSensorMapper {
    private NleSensorMapper() {
    }

    public static EnvironmentData.Sample parse(JSONObject response) throws Exception {
        if (response.optInt("Status", -1) != 0) {
            throw new IOException("云端未返回成功结果");
        }
        JSONArray sensors = response.optJSONArray("ResultObj");
        if (sensors == null) {
            throw new IOException("传感器列表缺失");
        }
        Map<String, JSONObject> byTag = new HashMap<>();
        for (int index = 0; index < sensors.length(); index++) {
            JSONObject sensor = sensors.getJSONObject(index);
            String tag = sensor.optString("ApiTag");
            if (byTag.put(tag, sensor) != null) {
                throw new IOException("传感器标识重复");
            }
        }
        String[] tags = {"Temperature", "Humidity", "Light"};
        double[] values = new double[3];
        long oldest = Long.MAX_VALUE;
        for (int index = 0; index < tags.length; index++) {
            JSONObject sensor = byTag.get(tags[index]);
            if (sensor == null || sensor.isNull("Value")) {
                throw new IOException(tags[index] + " 数据尚未上报");
            }
            values[index] = sensor.getDouble("Value");
            oldest = Math.min(oldest, parseTime(sensor.getString("RecordTime")));
        }
        return new EnvironmentData.Sample(oldest, values[0], values[1], values[2]);
    }

    private static long parseTime(String value) throws IOException {
        // The existing API returns local China time without an offset.
        String[] patterns = {"yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX"};
        for (String pattern : patterns) {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ROOT);
            format.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
            format.setLenient(false);
            ParsePosition position = new ParsePosition(0);
            Date date = format.parse(value, position);
            if (date != null && position.getIndex() == value.length() && date.getTime() > 0) {
                return date.getTime();
            }
        }
        throw new IOException("采样时间格式无法识别");
    }
}
