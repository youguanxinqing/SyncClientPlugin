package com.youguan.config;

import com.moandjiezana.toml.Toml;

public class ClientConfig {
    private String protocol;
    private long timeout; // 超时时间（秒）

    public ClientConfig(Toml toml) {
        this.protocol = toml.getString("client.protocol", "http");
        this.timeout = toml.getLong("client.timeout", 30L);
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    public long getTimeout() {
        return timeout;
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}