
package com.example.iot3.network;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NleCloudApi {

    private static final String BASE_URL =
            "https://api.nlecloud.com";

    // 1. 构造登录 JSON
    public JSONObject buildLoginJson(
            String account,
            String password
    ) throws Exception {

        JSONObject json = new JSONObject();

        json.put("Account", account);
        json.put("Password", password);
        json.put("IsRememberMe", false);

        return json;
    }

    // 2. 登录 NLECloud，返回 AccessToken
    public String login(
            String account,
            String password
    ) throws Exception {

        URL url = new URL(BASE_URL + "/Users/Login");

        HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();

        try {
            // 设置 HTTP POST
            conn.setRequestMethod("POST");

            conn.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
            );

            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);

            // 3. 构造登录请求体
            JSONObject json =
                    buildLoginJson(account, password);

            // 4. 向服务器发送 JSON
            try (OutputStream os = conn.getOutputStream()) {
                os.write(
                        json.toString().getBytes(
                                StandardCharsets.UTF_8)
                );
            }

            // 5. 获取 HTTP 状态码
            int statusCode = conn.getResponseCode();

            // 6. 读取服务器响应
            InputStream stream =
                    statusCode >= 200 && statusCode < 300
                            ? conn.getInputStream()
                            : conn.getErrorStream();

            if (stream == null) {
                throw new IOException(
                        "No response body, HTTP " + statusCode);
            }

            StringBuilder response = new StringBuilder();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         stream,
                                         StandardCharsets.UTF_8))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            // 7. 将服务器响应解析成 JSON
            JSONObject result =
                    new JSONObject(response.toString());

            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException(
                        "HTTP " + statusCode + ": "
                                + result.optString("Msg", ""));
            }

            // 8. 判断 NLECloud 登录状态
            int status = result.optInt("Status", -1);

            String msg = result.optString("Msg", "");

            if (status != 0) {
                throw new IOException(
                        "NLECloud login failed: " + msg);
            }

            // 9. 提取 ResultObj
            JSONObject resultObj =
                    result.optJSONObject("ResultObj");

            if (resultObj == null) {
                throw new IOException(
                        "ResultObj is missing");
            }

            // 10. 获取 AccessToken
            String token =
                    resultObj.optString("AccessToken", "");

            if (token.isEmpty()) {
                throw new IOException(
                        "AccessToken is empty");
            }

            // 11. 返回 Token
            return token;

        } finally {
            // 12. 释放 HTTP 连接
            conn.disconnect();
        }
    }

    public JSONObject getSensors(String token) throws Exception {

        URL url = new URL(
                BASE_URL +
                        "/devices/" + com.example.iot3.BuildConfig.NLE_DEVICE_ID + "/Sensors?apiTags=Temperature,Humidity,Light"
        );

        HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("AccessToken", token);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int code = conn.getResponseCode();

            InputStream stream = code >= 200 && code < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            if (stream == null) {
                throw new IOException("HTTP " + code + ": no response");
            }

            StringBuilder response = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            stream, StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JSONObject result = new JSONObject(response.toString());

            if (code < 200 || code >= 300
                    || result.optInt("Status", -1) != 0) {
                throw new IOException(
                        "Query failed: " + result.optString("Msg", ""));
            }

            return result;

        } finally {
            conn.disconnect();
        }
    }

}
