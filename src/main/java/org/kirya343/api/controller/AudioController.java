package org.kirya343.api.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@RestController
public class AudioController {

    @GetMapping("/audio")
    public ResponseEntity<InputStreamResource> getAudio() throws IOException {
        File audioFile = new File("music/Koda_-_Radioactive.mp3"); // путь к вашему файлу
        FileInputStream fis = new FileInputStream(audioFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + audioFile.getName())
                .contentLength(audioFile.length())
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(new InputStreamResource(fis));
    }
}