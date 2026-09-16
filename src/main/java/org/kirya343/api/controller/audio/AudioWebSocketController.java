package org.kirya343.api.controller.audio;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import java.util.Arrays;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequiredArgsConstructor 
public class AudioWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/audio/get")
    public void getAudioChunk(Principal principal) throws IOException, InterruptedException {

        // File file = new File("music/Не все дома.mp3");

        // byte[] audio = Files.readAllBytes(file.toPath());

        // log.info("Audio size: {}", audio.length);

        // // Примерно 128 KB/s для MP3 1024 kbps.
        // // Для теста можно подобрать размер экспериментально.
        // int chunkSize = 256 * 1024;

        // // Отправляем каждые 4 секунды
        // for (int offset = 0; offset < audio.length; offset += chunkSize) {

        //     int length = Math.min(chunkSize, audio.length - offset);

        //     byte[] chunk = Arrays.copyOfRange(
        //         audio,
        //         offset,
        //         offset + length
        //     );

        //     log.info(
        //         "Sending chunk: offset={}, size={}",
        //         offset,
        //         chunk.length
        //     );

        //     messagingTemplate.convertAndSendToUser(
        //         principal.getName(),
        //         "/queue/audio",
        //         chunk
        //     );

        //     Thread.sleep(4000);
        // }
    }
}
