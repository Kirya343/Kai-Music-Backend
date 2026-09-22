package org.kirya343.features.audio.services.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.kirya343.features.audio.services.LimitedInputStream;
import org.kirya343.features.audio.services.util.AudioConverter;
import org.kirya343.features.audio.services.util.Fmp4Parser;
import org.kirya343.features.audio.datasource.model.AudioFile;
import org.kirya343.features.user.datasource.User;
import org.kirya343.features.audio.datasource.repository.AudioFileRepository;
import org.kirya343.features.audio.dto.AudioChunk;
import org.kirya343.features.audio.dto.AudioMetadataDTO;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j 
@RequiredArgsConstructor
public class AudioFileManager {

    private final AudioFileRepository audioFileRepository;
    private final AudioStorageService audioStorageService;
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

    public void uploadAudio(MultipartFile uploadedFile, UserAuthData authData) {

        log.debug("Пользователь {} загружает аудио на сервер", authData.name());

        File tempInput = null;
        File converted = null;

        try {
            // 1. Multipart → File
            tempInput = AudioConverter.multipartToFile(uploadedFile);
            log.debug("Конвертировали из MultipartFile в File");

            AudioMetadataDTO metadata = AudioConverter.getMetadata(tempInput);

            log.debug("Полученные метаданные файла: {}", metadata.toString());

            // 2. Конвертация → MP3
            converted = AudioConverter.convertToMp3(tempInput);
            log.debug("Конвертировали в mp3");

            // 3. Название
            String originalName = uploadedFile.getOriginalFilename();

            log.debug("Оригинальное название файла: {}", originalName);

            if (originalName != null && originalName.contains(".")) {
                originalName = originalName.substring(0, originalName.lastIndexOf("."));
            }

            Fmp4Parser parser = new Fmp4Parser(converted.toPath());

            String audioDirectory = audioStorageService.createAudioDirectory();

            audioStorageService.saveInitializationChunk(
                audioDirectory,
                parser.getInitializationChunk()
            );

            List<AudioChunk> chunks = parser.getAudioChunks();
            for (AudioChunk chunk : chunks) {
                audioStorageService.saveChunk(
                    audioDirectory,
                    chunk
                );
            }

            String title = metadata.title().startsWith("upload") ? originalName : metadata.title();

            // 5. Сохраняем в БД
            AudioFile audio = new AudioFile(
                title,
                audioDirectory,
                "mp3",
                entityManager.getReference(User.class, authData.id()),
                metadata.title(),
                metadata.artist(),
                metadata.album(),
                null,
                metadata.durationMs() / 1000,
                chunks.size()
            );
            
            log.debug("Сохранили файл в бд");

            audioFileRepository.save(audio);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки аудио", e);
        } finally {
            // 6. Чистим временные файлы
            if (tempInput != null && tempInput.exists()) tempInput.delete();
            if (converted != null && converted.exists()) converted.delete();
        }
    }
}
