package com.example.lorameshapi.node;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class ConfigService {

    private final ConfigRepository configRepository;

    public Config get() {
        var config = configRepository.findById(1).orElseGet(Config::new);
        if (config.id == null) {
            config = configRepository.save(config);
        }
        return config;
    }
}
