package org.kirya343.features.audio.services.recognition;

import com.acrcloud.utils.ACRCloudRecognizer;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class AcrCloudRecognitionService {

    private final ACRCloudRecognizer recognizer;

    public AcrCloudRecognitionService(
            AcrCloudProperties properties
    ) {
        Map<String, Object> config = new HashMap<>();

        config.put("host", properties.host());
        config.put("access_key", properties.accessKey());
        config.put("access_secret", properties.accessSecret());
        config.put("debug", false);
        config.put(
                "timeout",
                Math.toIntExact(properties.timeout().toSeconds())
        );

        this.recognizer = new ACRCloudRecognizer(config);
    }

    public String recognize(byte[] m4a) {
        return recognizer.recognizeByFileBuffer(
                m4a,
                m4a.length,
                0
        );
    }
}