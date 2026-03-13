package org.kirya343.api.controller;

import org.kirya343.datasource.model.user.audio.ListeningRoom;
import org.kirya343.datasource.model.user.audio.QueueItem;
import org.kirya343.datasource.repository.audio.ListeningRoomRepository;
import org.kirya343.datasource.repository.audio.QueueItemRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/audio")
@RequiredArgsConstructor
public class AudioController {

    private final ListeningRoomRepository listeningRoomRepository;
    private final QueueItemRepository queueItemRepository;

    @GetMapping("/room/{roomId}")
    public ResponseEntity<InputStreamResource> getAudio(@PathVariable Long roomId) throws IOException {

        ListeningRoom room = listeningRoomRepository.findById(roomId).orElseThrow(
            () -> new EntityNotFoundException("Комната не найдена"));

        QueueItem item = queueItemRepository.findFirstByRoomOrderByPositionAsc(room).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Очередь пуста"));

        File audioFile = new File(item.getAudio().getPath()); // путь к вашему файлу
        FileInputStream fis = new FileInputStream(audioFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + audioFile.getName())
                .contentLength(audioFile.length())
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(new InputStreamResource(fis));
    }
}