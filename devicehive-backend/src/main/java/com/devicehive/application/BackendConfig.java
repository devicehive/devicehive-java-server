package com.devicehive.application;

import com.devicehive.json.GsonFactory;
import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackendConfig {

    @Bean
    public Gson gson() {
        return GsonFactory.createGson();
    }
}
