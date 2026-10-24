package com.example.iot3.network;

import com.example.iot3.data.EnvironmentData;

/** Reuses the user's login/query implementation; token remains in memory across polls. */
public final class NleCloudSource implements ReadingSource {
    private final NleCloudApi api = new NleCloudApi();
    private final String account;
    private final String password;
    private String token;

    public NleCloudSource(String account, String password) {
        this.account = account;
        this.password = password;
    }

    @Override
    public EnvironmentData.Sample read() throws Exception {
        try {
            if (token == null) {
                token = api.login(account, password);
            }
            return NleSensorMapper.parse(api.getSensors(token));
        } catch (Exception failure) {
            // Retry login on the next scheduled request, without an immediate retry loop.
            token = null;
            throw failure;
        }
    }
}
