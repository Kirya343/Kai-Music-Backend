package org.kirya343.api.websocket;

import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

@Component
public class AudioWebSocketHandler extends BinaryWebSocketHandler {

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Можно использовать текст для управления плейером (start/stop)
    }

    @Override
    protected void afterConnectionEstablished(WebSocketSession session) throws IOException {
        FileInputStream fis = new FileInputStream("path/to/audio.mp3");
        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = fis.read(buffer)) != -1) {
            session.sendMessage(new BinaryMessage(buffer, 0, bytesRead));
        }

        fis.close();
    }
}