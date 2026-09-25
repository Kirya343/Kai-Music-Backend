package org.kirya343.features.audio.services.recognition;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "acrcloud")
public record AcrCloudProperties(
        String host,
        String accessKey,
        String accessSecret,
        Duration timeout
) {
}
