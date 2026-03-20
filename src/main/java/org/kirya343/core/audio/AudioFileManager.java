package org.kirya343.core.audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.kirya343.datasource.model.audio.AudioFile;
import org.kirya343.datasource.model.user.User;
import org.kirya343.datasource.repository.audio.AudioFileRepository;
import org.kirya343.dto.auth.UserAuthData;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AudioFileManager {

    private final AudioFileRepository audioFileRepository;
    private final EntityManager entityManager;
    
    public ResponseEntity<InputStreamResource> getAudio(
        Long queueItemId, 
        String rangeHeader, 
        UserAuthData authData
    ) throws IOException {

        AudioFile audio = audioFileRepository.findAudioInUserRoom(authData.id(), queueItemId)
                .orElseThrow(() -> new AccessDeniedException("Нет доступа к этому треку"));

        File audioFile = new File(audio.getPath());
        long fileLength = audioFile.length();

        String contentType = Files.probeContentType(audioFile.toPath());
        System.out.println("contentType: " + contentType);
        if (contentType == null) {
            contentType = "application/octet-stream"; // fallback
        }

        if (rangeHeader == null) {
            // Отдаём весь файл целиком
            FileInputStream fis = new FileInputStream(audioFile);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + audioFile.getName())
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentLength(fileLength)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new InputStreamResource(fis));
        }

        // Парсим Range: bytes=START-
        String[] ranges = rangeHeader.replace("bytes=", "").split("-");
        long start = Long.parseLong(ranges[0]);
        long end = ranges.length > 1 && !ranges[1].isEmpty() ? Long.parseLong(ranges[1]) : fileLength - 1;
        long contentLength = end - start + 1;

        FileInputStream fis = new FileInputStream(audioFile);
        fis.skip(start);
        InputStreamResource resource = new InputStreamResource(new LimitedInputStream(fis, contentLength));

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + audioFile.getName())
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                .body(resource);
    }

    public void updloadAudio(MultipartFile file, UserAuthData authData) {
        String originalName = file.getOriginalFilename();
        String title = originalName;
        String extension = "";

        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        if (originalName != null && originalName.contains(".")) {
            title = originalName.substring(0, originalName.lastIndexOf("."));
        }

        String name = UUID.randomUUID().toString() + extension;

        try {
            Path path = Paths.get("music/" + name);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            AudioFile audio = new AudioFile(
                title, 
                "music/" + name, 
                entityManager.getReference(User.class, authData.id())
            );

            audioFileRepository.save(audio);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
